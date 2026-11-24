package mg.lagrace.api.booking.application;

import mg.lagrace.api.booking.core.BookingOrigin;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.reference.Unit;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.pool.BookingPool;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.pool.PoolRate;
import mg.lagrace.api.catalog.pool.PoolService;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.pool.BookingPoolExtendRequest;
import mg.lagrace.api.booking.dto.pool.BookingPoolUpdateRequest;
import mg.lagrace.api.booking.dto.room.BookingResponse;
import mg.lagrace.api.booking.dto.pool.BookingPoolRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.booking.pool.persistence.BookingPoolRepository;
import mg.lagrace.api.catalog.pool.persistence.PoolRateRepository;
import mg.lagrace.api.catalog.pool.persistence.PoolRepository;
import mg.lagrace.api.catalog.pool.persistence.PoolServiceRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class BookingPoolService {
    @Autowired private BookingPermissionService permissions;
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private final BookingRoomService common;
    private final BookingRepository bookingRepository;
    private final BookingPoolRepository bookingPoolRepository;
    private final BookingBalanceRepository bookingBalanceRepository;
    private final PoolRepository poolRepository;
    private final PoolServiceRepository poolServiceRepository;
    private final PoolRateRepository poolRateRepository;
    private final UserRepository userRepository;
    private final BookingDetailService bookingDetails;
    private final BookingChangeLogger changeLogger;
    private final BookingExtraService extraService;
    private final Clock clock;
    @PersistenceContext
    private EntityManager entityManager;

    public BookingPoolService(BookingRoomService common, BookingRepository bookingRepository,
            BookingPoolRepository bookingPoolRepository, BookingBalanceRepository bookingBalanceRepository,
            PoolRepository poolRepository, PoolServiceRepository poolServiceRepository,
            PoolRateRepository poolRateRepository,
            UserRepository userRepository, BookingDetailService bookingDetails,
            BookingChangeLogger changeLogger, BookingExtraService extraService, Clock clock) {
        this.common = common;
        this.bookingRepository = bookingRepository;
        this.bookingPoolRepository = bookingPoolRepository;
        this.bookingBalanceRepository = bookingBalanceRepository;
        this.poolRepository = poolRepository;
        this.poolServiceRepository = poolServiceRepository;
        this.poolRateRepository = poolRateRepository;
        this.userRepository = userRepository;
        this.bookingDetails = bookingDetails;
        this.changeLogger = changeLogger;
        this.extraService = extraService;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse update(Long id, BookingPoolUpdateRequest request, Authentication authentication) {
        BookingPool line = bookingPoolRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne piscine a été modifiée par quelqu'un d'autre.");
        if (booking == null || booking.getStatus() == null || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (line.getStatus() == null || !"CONFIRMED".equals(line.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être modifiée.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        Pool pool = request.poolId() == null ? line.getPool() : poolRepository.findById(request.poolId()).orElseThrow(() -> new ResourceNotFoundException("Piscine introuvable."));
        PoolService service = request.poolServiceId() == null ? line.getPoolService() : poolServiceRepository.findById(request.poolServiceId()).orElseThrow(() -> new ResourceNotFoundException("Formule piscine introuvable."));
        OffsetDateTime starts = request.startsAt() == null ? line.getStartsAt() : request.startsAt();
        OffsetDateTime ends = request.endsAt() == null ? line.getEndsAt() : request.endsAt();
        boolean exclusive = Boolean.TRUE.equals(service.getIsExculusive());
        if (starts == null || starts.toInstant().isBefore(OffsetDateTime.now(clock).toInstant())) throw new ValidationException("La date de début ne peut pas être dans le passé.", "startsAt");
        validateSlot(exclusive, starts, ends);
        Integer guests = request.guestsCount() == null ? line.getGuestsCount() : request.guestsCount();
        if (guests == null || guests < 1) throw new ValidationException("Le nombre de personnes est obligatoire.", "guestsCount");
        if (pool.getCapacity() != null && guests > pool.getCapacity()) throw new UnprocessableEntityException("VALIDATION", "La capacité de la piscine est de " + pool.getCapacity() + " personnes.", "guestsCount");
        LocalDate rateDate = starts.toInstant().atZone(MADAGASCAR).toLocalDate();
        boolean reprice = request.poolId() != null || request.poolServiceId() != null || request.startsAt() != null || request.poolRateId() != null;
        PoolRate rate = reprice ? resolveRate(request.poolRateId() == null ? line.getPoolRate().getIdRate() : request.poolRateId(), pool, service, rateDate, "poolRateId") : line.getPoolRate();
        int quantity = request.lines() != null && !request.lines().isEmpty() ? request.lines().get(0).quantity() : line.getQuantity();
        if (quantity < 1) throw new ValidationException("La quantité doit être positive.", "quantity");
        if (exclusive) quantity = quantityFor(rate, quantity, guests, starts, ends, true);
        if (bookingPoolRepository.existsConflictingBooking(pool.getIdPool(), booking.getIdBooking(), exclusive, starts, ends))
            throw new ConflictException("POOL_UNAVAILABLE", "La piscine est indisponible sur ce créneau.");
        BigDecimal oldTotal = bookingBalanceRepository.findById(booking.getIdBooking()).orElseThrow().getTotal();
        java.util.UUID operation = changeLogger.beginOperation();
        try {
            logPoolChange(booking, line, "pool_id", line.getPool().getIdPool(), pool.getIdPool(), request.reason(), user, operation);
            logPoolChange(booking, line, "pool_service_id", line.getPoolService().getIdService(), service.getIdService(), request.reason(), user, operation);
            logPoolChange(booking, line, "starts_at", line.getStartsAt(), starts, request.reason(), user, operation);
            logPoolChange(booking, line, "ends_at", line.getEndsAt(), ends, request.reason(), user, operation);
            logPoolChange(booking, line, "guests_count", line.getGuestsCount(), guests, request.reason(), user, operation);
            logPoolChange(booking, line, "quantity", line.getQuantity(), quantity, request.reason(), user, operation);
            logPoolChange(booking, line, "pool_rate_id", line.getPoolRate().getIdRate(), rate.getIdRate(), request.reason(), user, operation);
            logPoolChange(booking, line, "unit_price", line.getUnitPrice(), reprice ? rate.getAmount() : line.getUnitPrice(), request.reason(), user, operation);
        } finally { changeLogger.endOperation(); }
        line.setPool(pool); line.setPoolService(service); line.setStartsAt(starts); line.setEndsAt(ends); line.setGuestsCount(guests); line.setQuantity(quantity); line.setPoolRate(rate);
        if (reprice) line.setUnitPrice(rate.getAmount());
        bookingPoolRepository.saveAndFlush(line);
        BigDecimal newTotal = bookingBalanceRepository.findById(booking.getIdBooking()).orElseThrow().getTotal();
        if (request.expectedNewTotal() != null && request.expectedNewTotal().compareTo(newTotal) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total recalculé a changé.", "expectedNewTotal", newTotal);
        BigDecimal delta = newTotal.subtract(oldTotal);
        return bookingDetails.get(booking.getIdBooking()).withAdjustment(delta, delta.signum() >= 0 ? "SUPPLEMENT" : "ECONOMIE");
    }

    private void logPoolChange(Booking booking, BookingPool line, String field, Object oldValue, Object newValue, String reason, User user, java.util.UUID operation) {
        if (!java.util.Objects.equals(oldValue, newValue)) changeLogger.log(booking.getIdBooking(), "POOL", line.getIdBookingPool(), "UPDATE", field, oldValue, newValue, reason, user.getIdUser(), operation);
    }

    @Transactional
    public BookingDetailResponse extend(Long id, BookingPoolExtendRequest request, Authentication authentication) {
        BookingPool line = bookingPoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        if (Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (!Boolean.TRUE.equals(line.getPoolService().getIsExculusive()))
            throw new ValidationException("Seule une privatisation de piscine peut être prolongée.");
        String lineStatus = line.getStatus() == null ? null : line.getStatus().getCode();
        if (!Set.of("CONFIRMED", "CHECK_IN").contains(lineStatus))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne piscine confirmée ou commencée peut être prolongée.");
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne piscine a été modifiée par quelqu'un d'autre.");
        if (request.newEndsAt() == null || line.getEndsAt() == null || !request.newEndsAt().isAfter(line.getEndsAt()))
            throw new ValidationException("La nouvelle fin doit être postérieure à la fin actuelle.", "newEndsAt");
        if (!request.newEndsAt().toInstant().isAfter(OffsetDateTime.now(clock).toInstant()))
            throw new ValidationException("La nouvelle fin doit être dans le futur.", "newEndsAt");
        if (authentication == null || authentication.getName() == null)
            throw new ResourceNotFoundException("Utilisateur authentifié introuvable.");
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));

        BigDecimal oldTotal = bookingBalanceRepository.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."))
                .getTotal();
        PoolRate rate = line.getPoolRate();
        int quantity = quantityFor(rate, line.getQuantity(), line.getGuestsCount(), line.getStartsAt(), request.newEndsAt(), true);
        if (bookingPoolRepository.existsConflictingBooking(line.getPool().getIdPool(), booking.getIdBooking(), true,
                line.getStartsAt(), request.newEndsAt())) {
            throw new ConflictException("POOL_UNAVAILABLE", "La piscine est indisponible sur cette période.");
        }

        OffsetDateTime oldEndsAt = line.getEndsAt();
        BigDecimal oldUnitPrice = line.getUnitPrice();
        Integer oldQuantity = line.getQuantity();
        Long oldRateId = line.getPoolRate().getIdRate();
        line.setEndsAt(request.newEndsAt());
        line.setPoolRate(rate);
        line.setUnitPrice(oldUnitPrice);
        line.setQuantity(quantity);
        bookingPoolRepository.saveAndFlush(line);
        entityManager.refresh(line);

        var balance = bookingBalanceRepository.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."));
        BigDecimal newTotal = balance.getTotal();
        if (request.expectedNewTotal() != null && request.expectedNewTotal().compareTo(newTotal) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total recalculé a changé.", "expectedNewTotal", newTotal);

        java.util.UUID operationId = changeLogger.beginOperation();
        try {
            Long userId = user.getIdUser();
            changeLogger.log(booking.getIdBooking(), "POOL", id, "UPDATE", "ends_at", oldEndsAt,
                    request.newEndsAt(), request.reason(), userId, operationId);
            changeLogger.log(booking.getIdBooking(), "POOL", id, "UPDATE", "pool_rate_id", oldRateId,
                    rate.getIdRate(), request.reason(), userId, operationId);
            changeLogger.log(booking.getIdBooking(), "POOL", id, "UPDATE", "quantity", oldQuantity,
                    quantity, request.reason(), userId, operationId);
            changeLogger.log(booking.getIdBooking(), "POOL", id, "UPDATE", "unit_price", oldUnitPrice,
                    oldUnitPrice, request.reason(), userId, operationId);
        } finally {
            changeLogger.endOperation();
        }
        BigDecimal delta = newTotal.subtract(oldTotal);
        return bookingDetails.get(booking.getIdBooking()).withAdjustment(delta,
                delta.signum() >= 0 ? "SUPPLEMENT" : "ECONOMIE");
    }

    @Transactional
    public BookingDetailResponse createBooking(BookingPoolRequest request, String username) {
        permissions.require(username, null, "BOOKING_UPDATE");
        if (request != null && Boolean.TRUE.equals(request.confirm())) permissions.require(username, null, "BOOKING_CONFIRM");
        validateRequest(request);
        var item = request.pool();
        OffsetDateTime startsAt = item.startsAt();
        OffsetDateTime endsAt = item.endsAt();
        LocalDate rateDate = startsAt.toInstant().atZone(MADAGASCAR).toLocalDate();
        LocalDate today = LocalDate.now(clock.withZone(MADAGASCAR));
        if (rateDate.isBefore(today)) throw new ValidationException("La date de début ne peut pas être dans le passé.", "pool.startsAt");

        Pool pool = poolRepository.findById(item.poolId())
                .orElseThrow(() -> new ResourceNotFoundException("Piscine introuvable avec l'id : " + item.poolId()));
        PoolService service = poolServiceRepository.findById(item.poolServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Formule piscine introuvable avec l'id : " + item.poolServiceId()));
        String formula = service.getCode().toUpperCase(Locale.ROOT);
        boolean simpleEntry = "ENTRY".equals(formula);
        boolean privatePool = "PRIVATE".equals(formula);
        boolean poolRestaurant = "POOL_RESTAURANT".equals(formula);
        if (!simpleEntry && !privatePool && !poolRestaurant)
            throw new ValidationException("Cette formule piscine n'est pas reconnue.", "pool.poolServiceId");
        if (Boolean.TRUE.equals(service.getIsExculusive()) != privatePool)
            throw new ValidationException("La configuration de la formule piscine est incohérente.", "pool.poolServiceId");
        validateSlot(privatePool, startsAt, endsAt);
        if ((privatePool || poolRestaurant) && (item.guestsCount() == null || item.guestsCount() < 1))
            throw new ValidationException("Le nombre de personnes est obligatoire pour cette formule.", "pool.guestsCount");
        if (item.guestsCount() != null && pool.getCapacity() != null && item.guestsCount() > pool.getCapacity())
            throw new UnprocessableEntityException("VALIDATION", "La capacité de la piscine est de " + pool.getCapacity() + " personnes.", "pool.guestsCount");
        if ((privatePool || poolRestaurant) && item.lines().size() != 1)
            throw new ValidationException("Cette formule exige une seule ligne tarifaire.", "pool.lines");
        if (simpleEntry && item.lines().stream().anyMatch(line -> line.poolRateId() == null))
            throw new ValidationException("Chaque tranche d'âge doit avoir un tarif.", "pool.lines");

        Set<Long> rateIds = new HashSet<>();
        Set<String> ageRanges = new HashSet<>();
        List<PoolRate> resolvedRates = new java.util.ArrayList<>();
        int entryGuests = 0;
        for (int index = 0; index < item.lines().size(); index++) {
            BookingPoolRequest.RateItem rateItem = item.lines().get(index);
            String field = "pool.lines[" + index + "]";
            if (rateItem.poolRateId() == null || rateItem.quantity() == null || rateItem.quantity() < 1)
                throw new ValidationException("Le tarif et une quantité positive sont obligatoires.", field);
            if (!rateIds.add(rateItem.poolRateId()))
                throw new ValidationException("Une ligne tarifaire ne peut apparaître qu'une seule fois.", field + ".poolRateId");
            PoolRate rate = resolveRate(rateItem.poolRateId(), pool, service, rateDate, field + ".poolRateId");
            if (simpleEntry) {
                if (rate.getIsMealIncluded() != null || (rate.getMinAge() == null && rate.getMaxAge() == null))
                    throw new ValidationException("L'entrée simple exige un tarif par tranche d'âge.", field + ".poolRateId");
                String ageRange = rate.getMinAge() + ":" + rate.getMaxAge();
                if (!ageRanges.add(ageRange))
                    throw new ValidationException("Les tranches d'âge doivent être distinctes.", field + ".poolRateId");
                entryGuests += rateItem.quantity();
            } else if (rate.getIsMealIncluded() == null || rate.getMinAge() != null || rate.getMaxAge() != null) {
                throw new ValidationException("Cette formule exige un tarif avec ou sans repas.", field + ".poolRateId");
            }
            if (privatePool && rateItem.quantity() != 1)
                throw new ValidationException("La privatisation a une quantité fixe de 1.", field + ".quantity");
            resolvedRates.add(rate);
        }
        Integer guestsCount = simpleEntry ? entryGuests : item.guestsCount();
        if (simpleEntry && item.guestsCount() != null && !item.guestsCount().equals(entryGuests))
            throw new ValidationException("Le nombre total doit être égal à la somme des tranches.", "pool.guestsCount");
        if (guestsCount == null || guestsCount < 1)
            throw new ValidationException("Sélectionnez au moins une personne.", "pool.guestsCount");
        if (pool.getCapacity() != null && guestsCount > pool.getCapacity())
            throw new UnprocessableEntityException("VALIDATION", "La capacité de la piscine est de " + pool.getCapacity() + " personnes.", "pool.guestsCount");

        checkAvailability(pool, startsAt, endsAt, privatePool, rateDate);
        Customer customer = common.handleCustomer(request.customerId(), request.newCustomer());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        BookingStatus bookingStatus = common.findBookingStatus(Boolean.TRUE.equals(request.confirm()) ? "CONFIRMED" : "PENDING");
        BookingLineStatus lineStatus = common.findLineStatus("CONFIRMED");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(request.origin() == null ? mg.lagrace.api.booking.core.BookingOrigin.LOCAL : request.origin());
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(bookingStatus);
        booking = bookingRepository.saveAndFlush(booking);

        for (int index = 0; index < item.lines().size(); index++) {
            BookingPoolRequest.RateItem rateItem = item.lines().get(index);
            PoolRate rate = resolvedRates.get(index);
            int quantity = quantityFor(rate, rateItem.quantity(), guestsCount, startsAt, endsAt, privatePool);
            BookingPool line = new BookingPool();
            line.setBooking(booking);
            line.setPool(pool);
            line.setPoolService(service);
            line.setPoolRate(rate);
            line.setStartsAt(startsAt);
            line.setEndsAt(privatePool ? endsAt : null);
            line.setGuestsCount(guestsCount);
            line.setQuantity(quantity);
            line.setUnitPrice(rate.getAmount());
            line.setIncluded(false);
            line.setStatus(lineStatus);
            line.setActive(true);
            line = bookingPoolRepository.saveAndFlush(line);
            common.saveHistory(booking, "POOL", line.getIdBookingPool(), lineStatus.getCode(), user);
        }

        extraService.addToBooking(booking, request.extras(), user, rateDate, "POOL");
        BigDecimal total = readTotal(booking.getIdBooking(), request.expectedTotal());
        common.saveHistory(booking, null, null, bookingStatus.getCode(), user);
        return bookingDetails.get(booking.getIdBooking());
    }

    private PoolRate resolveRate(Long rateId, Pool pool, PoolService service, LocalDate date, String field) {
        List<PoolRate> applicableRates = poolRateRepository.findApplicableByPoolAndService(
                pool.getIdPool(), service.getIdService(), date);
        if (applicableRates.isEmpty())
            throw new UnprocessableEntityException("NO_TARIFF", "Aucun tarif applicable à cette date.", field);
        PoolRate rate = poolRateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif piscine introuvable : " + rateId));
        if (!rate.getPool().getIdPool().equals(pool.getIdPool())
                || !rate.getService().getIdService().equals(service.getIdService())) {
            throw new UnprocessableEntityException("RATE_NOT_APPLICABLE", "Le tarif ne correspond pas à la piscine et à la formule.", field);
        }
        boolean applicable = applicableRates.stream()
                .anyMatch(candidate -> candidate.getIdRate().equals(rateId));
        if (!applicable) {
            throw new UnprocessableEntityException("RATE_NOT_APPLICABLE", "Le tarif piscine n'est pas applicable à la date demandée.", field);
        }
        return rate;
    }

    private int quantityFor(PoolRate rate, int requested, Integer guests, OffsetDateTime startsAt,
            OffsetDateTime endsAt, boolean exclusive) {
        String unit = rate.getUnit() == null || rate.getUnit().getName() == null
                ? "" : rate.getUnit().getName().trim().toLowerCase(Locale.ROOT);
        if ("ENTRY".equalsIgnoreCase(rate.getService().getCode())) return requested;
        if (unit.equals("forfait")) return 1;
        if (unit.equals("par personne")) {
            if (guests == null) throw new ValidationException("Le nombre de personnes est obligatoire pour ce tarif.");
            return guests;
        }
        if (unit.equals("heure")) {
            if (!exclusive || endsAt == null) throw new ValidationException("La durée est obligatoire pour un tarif à l'heure.");
            long milliseconds = Duration.between(startsAt, endsAt).toMillis();
            return (int) Math.ceil(milliseconds / 3_600_000.0);
        }
        return requested;
    }

    private BigDecimal readTotal(Long bookingId, BigDecimal expectedTotal) {
        BookingBalance balance = bookingBalanceRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."));
        BigDecimal total = balance.getTotal();
        if (expectedTotal != null && expectedTotal.compareTo(total) != 0) {
            throw new ConflictException("TOTAL_CHANGED", "Le total calculé est " + total + " Ar. Vérifiez le récapitulatif.", "expectedTotal", total);
        }
        return total;
    }

    private void validateRequest(BookingPoolRequest request) {
        if (request == null || (request.customerId() == null) == (request.newCustomer() == null))
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        if (request.pool() == null || request.pool().poolId() == null || request.pool().poolServiceId() == null
                || request.pool().startsAt() == null || request.pool().lines() == null || request.pool().lines().isEmpty())
            throw new ValidationException("Au moins une ligne de formule piscine est requise.");
    }

    private void checkAvailability(Pool pool, OffsetDateTime startsAt, OffsetDateTime endsAt,
            boolean privatePool, LocalDate date) {
        boolean hasHour = !startsAt.toInstant().atZone(MADAGASCAR).toLocalTime().equals(java.time.LocalTime.MIDNIGHT);
        OffsetDateTime from = privatePool || hasHour ? startsAt : date.atStartOfDay(MADAGASCAR).toOffsetDateTime();
        OffsetDateTime to = privatePool ? endsAt : hasHour ? startsAt.plusNanos(1)
                : date.plusDays(1).atStartOfDay(MADAGASCAR).toOffsetDateTime();
        List<BookingPool> conflicts = bookingPoolRepository.findAvailabilityConflicts(
                pool.getIdPool(), from, to, null);
        if (privatePool) {
            if (conflicts.stream().anyMatch(line -> !Boolean.TRUE.equals(line.getPoolService().getIsExculusive()))) {
                throw new ConflictException("POOL_HAS_ENTRIES", "Des entrées piscine existent déjà dans ce créneau.", "pool.endsAt", null);
            }
            if (!conflicts.isEmpty())
                throw new ConflictException("POOL_UNAVAILABLE", "Une privatisation existe déjà dans ce créneau.", "pool.startsAt", null);
            return;
        }
        BookingPool privateConflict = conflicts.stream()
                .filter(line -> Boolean.TRUE.equals(line.getPoolService().getIsExculusive()))
                .findFirst().orElse(null);
        if (privateConflict == null) return;
        String fromTime = privateConflict.getStartsAt().toInstant().atZone(MADAGASCAR).toLocalTime().toString();
        String toTime = privateConflict.getEndsAt().toInstant().atZone(MADAGASCAR).toLocalTime().toString();
        if (!hasHour)
            throw new ConflictException("HOUR_REQUIRED", "Piscine privatisée de " + fromTime + " à " + toTime + ". Précisez l'heure.", "pool.startsAt", null);
        throw new ConflictException("POOL_PRIVATIZED", "La piscine est privatisée de " + fromTime + " à " + toTime + ".", "pool.startsAt", null);
    }

    private void validateSlot(boolean exclusive, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (exclusive && (endsAt == null || !endsAt.isAfter(startsAt)))
            throw new ValidationException("Une formule exclusive exige une fin après le début.");
        if (!exclusive && endsAt != null)
            throw new ValidationException("Une formule d'entrée ne doit pas avoir de date de fin.");
    }
}
