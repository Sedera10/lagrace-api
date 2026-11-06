package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.booking.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.dto.booking.room.BookingResponse;
import mg.lagrace.api.dto.booking.pool.BookingPoolRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingCharge;
import mg.lagrace.api.models.BookingLineStatus;
import mg.lagrace.api.models.BookingPool;
import mg.lagrace.api.models.BookingStatus;
import mg.lagrace.api.models.Customer;
import mg.lagrace.api.models.MenuItem;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.models.Payment;
import mg.lagrace.api.models.PaymentMethod;
import mg.lagrace.api.models.Pool;
import mg.lagrace.api.models.PoolRate;
import mg.lagrace.api.models.PoolService;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BookingPoolRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.MenuItemRateRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import mg.lagrace.api.repositories.PaymentKindRepository;
import mg.lagrace.api.repositories.PaymentMethodRepository;
import mg.lagrace.api.repositories.PaymentRepository;
import mg.lagrace.api.repositories.PoolRateRepository;
import mg.lagrace.api.repositories.PoolRepository;
import mg.lagrace.api.repositories.PoolServiceRepository;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.models.views.BookingBalance;

@Service
public class BookingPoolService {
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private static final String POOL_SCOPE = "POOL";

    private final BookingRoomService common;
    private final BookingRepository bookingRepository;
    private final BookingPoolRepository bookingPoolRepository;
    private final BookingChargeRepository bookingChargeRepository;
    private final BookingBalanceRepository bookingBalanceRepository;
    private final PoolRepository poolRepository;
    private final PoolServiceRepository poolServiceRepository;
    private final PoolRateRepository poolRateRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemRateRepository menuItemRateRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentKindRepository paymentKindRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public BookingPoolService(BookingRoomService common, BookingRepository bookingRepository,
            BookingPoolRepository bookingPoolRepository, BookingChargeRepository bookingChargeRepository,
            BookingBalanceRepository bookingBalanceRepository, PoolRepository poolRepository,
            PoolServiceRepository poolServiceRepository, PoolRateRepository poolRateRepository,
            AdditionalServiceRepository additionalServiceRepository, MenuItemRepository menuItemRepository,
            MenuItemRateRepository menuItemRateRepository, PaymentMethodRepository paymentMethodRepository,
            PaymentKindRepository paymentKindRepository, PaymentRepository paymentRepository,
            UserRepository userRepository) {
        this.common = common;
        this.bookingRepository = bookingRepository;
        this.bookingPoolRepository = bookingPoolRepository;
        this.bookingChargeRepository = bookingChargeRepository;
        this.bookingBalanceRepository = bookingBalanceRepository;
        this.poolRepository = poolRepository;
        this.poolServiceRepository = poolServiceRepository;
        this.poolRateRepository = poolRateRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemRateRepository = menuItemRateRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentKindRepository = paymentKindRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingPoolRequest request, String username) {
        validateRequest(request);
        OffsetDateTime startsAt = request.pool().startsAt() == null
                ? OffsetDateTime.now(MADAGASCAR) : request.pool().startsAt();
        OffsetDateTime endsAt = request.pool().endsAt();
        LocalDate rateDate = startsAt.toInstant().atZone(MADAGASCAR).toLocalDate();

        Pool pool = poolRepository.findById(request.pool().poolId())
                .orElseThrow(() -> new ResourceNotFoundException("Piscine introuvable."));
        PoolService service = poolServiceRepository.findById(request.pool().poolServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Formule piscine introuvable."));
        boolean exclusive = Boolean.TRUE.equals(service.getIsExculusive());
        validateSlot(exclusive, startsAt, endsAt);
        if (startsAt.toInstant().atZone(MADAGASCAR).toLocalDate()
                .isBefore(LocalDate.now(MADAGASCAR))) {
            throw new ValidationException("La date de début ne peut pas être dans le passé.");
        }
        if (request.pool().guestsCount() != null && pool.getCapacity() != null
                && request.pool().guestsCount() > pool.getCapacity()) {
            throw new UnprocessableEntityException("La capacité de la piscine est de " + pool.getCapacity() + " personnes.");
        }
        if (!exclusive) {
            endsAt = startsAt.plusNanos(1);
        }
        if (bookingPoolRepository.existsConflictingBooking(pool.getIdPool(), -1L, exclusive, startsAt, endsAt)) {
            throw new ConflictException("POOL_ALREADY_BOOKED", "La piscine n'est pas disponible pour ce créneau.");
        }

        Set<Long> rateIds = new HashSet<>();
        Customer customer = common.handleCustomer(request.customerId(), request.newCustomer());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        BookingStatus pending = common.findBookingStatus("PENDING");
        BookingLineStatus confirmedLine = common.findLineStatus("CONFIRMED");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(request.origin() == null ? mg.lagrace.api.models.enums.BookingOrigin.LOCAL : request.origin());
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(pending);
        booking = bookingRepository.saveAndFlush(booking);

        for (BookingPoolRequest.RateItem item : request.pool().lines()) {
            if (!rateIds.add(item.poolRateId())) {
                throw new ValidationException("Un tarif piscine ne peut apparaître qu'une seule fois.");
            }
            PoolRate rate = resolveRate(item.poolRateId(), pool, service, rateDate);
                int requestedQuantity = request.pool().lines().size() > 1
                    ? item.quantity()
                    : request.pool().guestsCount() == null ? item.quantity() : request.pool().guestsCount();
                int quantity = quantityFor(rate, requestedQuantity, request.pool().guestsCount(), startsAt, endsAt, exclusive);
            BookingPool line = new BookingPool();
            line.setBooking(booking);
            line.setPool(pool);
            line.setPoolService(service);
            line.setPoolRate(rate);
            line.setStartsAt(startsAt);
            line.setEndsAt(exclusive ? endsAt : null);
            line.setGuestsCount(request.pool().guestsCount());
            line.setQuantity(quantity);
            line.setUnitPrice(rate.getAmount());
            line.setIncluded(false);
            line.setStatus(confirmedLine);
            line.setActive(true);
            line = bookingPoolRepository.saveAndFlush(line);
            common.saveHistory(booking, "POOL", line.getIdBookingPool(), confirmedLine.getCode(), user);
        }

        createExtras(booking, request.extras(), user, rateDate);
        PaymentMethod method = request.payment() == null ? null : paymentMethodRepository.findById(request.payment().methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable."));
        BigDecimal total = readTotal(booking.getIdBooking(), request.expectedTotal());
        if (request.payment() != null && request.payment().amount().compareTo(total) > 0) {
            throw new UnprocessableEntityException("Le paiement ne peut pas dépasser le total de la réservation.");
        }
        if (request.payment() != null) {
            String kindCode = request.payment().kind() == null || request.payment().kind().isBlank()
                    ? "ADVANCE" : request.payment().kind().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("ADVANCE", "PAYMENT").contains(kindCode)) {
                throw new ValidationException("Le type de paiement piscine doit être ADVANCE ou PAYMENT.");
            }
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(method);
            payment.setKind(paymentKindRepository.findByCode(kindCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable : " + kindCode)));
            payment.setAmount(request.payment().amount());
            payment.setReceivedByUser(user);
            paymentRepository.saveAndFlush(payment);
        }
        boolean confirmed = request.payment() != null || Boolean.TRUE.equals(request.confirm());
        BookingStatus finalStatus = common.findBookingStatus(confirmed ? "CONFIRMED" : "PENDING");
        booking.setStatus(finalStatus);
        bookingRepository.saveAndFlush(booking);
        common.saveHistory(booking, null, null, finalStatus.getCode(), user);
        return common.getBooking(booking.getIdBooking());
    }

    private PoolRate resolveRate(Long rateId, Pool pool, PoolService service, LocalDate date) {
        if (rateId == null) {
            List<PoolRate> applicableRates = poolRateRepository.findApplicableByPoolAndService(
                    pool.getIdPool(), service.getIdService(), date);
            if (applicableRates.size() != 1) {
                throw new UnprocessableEntityException(
                        "La formule " + service.getName()
                                + " possède plusieurs tarifs applicables. Une répartition par âge est nécessaire.");
            }
            return applicableRates.get(0);
        }
        PoolRate rate = poolRateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif piscine introuvable : " + rateId));
        if (!rate.getPool().getIdPool().equals(pool.getIdPool())
                || !rate.getService().getIdService().equals(service.getIdService())) {
            throw new UnprocessableEntityException("Le tarif piscine ne correspond pas à la piscine et à la formule.");
        }
        boolean applicable = poolRateRepository.findApplicableByPoolAndService(
                pool.getIdPool(), service.getIdService(), date).stream()
                .anyMatch(candidate -> candidate.getIdRate().equals(rateId));
        if (!applicable) {
            throw new UnprocessableEntityException("Le tarif piscine n'est pas applicable à la date demandée.");
        }
        return rate;
    }

    private int quantityFor(PoolRate rate, int requested, Integer guests, OffsetDateTime startsAt,
            OffsetDateTime endsAt, boolean exclusive) {
        String unit = rate.getUnit() == null || rate.getUnit().getName() == null
                ? "" : rate.getUnit().getName().trim().toLowerCase(Locale.ROOT);
        if (unit.equals("forfait")) return 1;
        if (unit.equals("par personne")) {
            if (guests == null) throw new ValidationException("Le nombre de personnes est obligatoire pour ce tarif.");
            return guests;
        }
        if (unit.equals("heure")) {
            if (!exclusive || endsAt == null) throw new ValidationException("La durée est obligatoire pour un tarif à l'heure.");
            long minutes = Duration.between(startsAt, endsAt).toMinutes();
            return (int) Math.ceil(minutes / 60.0);
        }
        return requested;
    }

    private BigDecimal readTotal(Long bookingId, BigDecimal expectedTotal) {
        BookingBalance balance = bookingBalanceRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."));
        BigDecimal total = balance.getTotal().add(balance.getDepositDue());
        if (expectedTotal != null && expectedTotal.compareTo(total) != 0) {
            throw new ConflictException("TOTAL_MISMATCH", "Le total calculé (" + total + ") ne correspond pas au total attendu (" + expectedTotal + ").");
        }
        return total;
    }

    private void createExtras(Booking booking, List<ExtraItem> extras, User user, LocalDate date) {
        if (extras == null) return;
        for (ExtraItem extra : extras) {
            if (extra == null || extra.quantity() == null || extra.quantity() < 1
                    || (extra.menuItemId() == null) == (extra.additionalServiceId() == null)) {
                throw new ValidationException("Chaque extra doit avoir une quantité positive et une seule source.");
            }
            BookingCharge charge = new BookingCharge();
            charge.setBooking(booking);
            charge.setQuantity(extra.quantity());
            charge.setIncluded(false);
            charge.setCreatedByUser(user);
            if (extra.additionalServiceId() != null) {
                AdditionalService service = additionalServiceRepository.findById(extra.additionalServiceId().longValue())
                        .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
                if (!service.isActive() || service.getScope() == null
                        || !POOL_SCOPE.equalsIgnoreCase(service.getScope().getCode())) {
                    throw new UnprocessableEntityException("Le service additionnel ne s'applique pas à la piscine.");
                }
                charge.setAdditionalService(service);
                charge.setLabel(service.getName());
                charge.setUnitPrice(service.getPrice());
                charge.setDeposit(service.isDeposit());
            } else {
                MenuItem item = menuItemRepository.findById(extra.menuItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Article du menu introuvable."));
                if (!Boolean.TRUE.equals(item.getActive())) throw new UnprocessableEntityException("L'article du menu n'est pas actif.");
                MenuItemRate rate = menuItemRateRepository.findApplicableByMenuItem(item.getIdMenuItem(), date);
                if (rate == null) throw new UnprocessableEntityException("Aucun prix applicable pour l'article du menu.");
                charge.setMenuItem(item);
                charge.setLabel(item.getName());
                charge.setUnitPrice(rate.getAmount());
                charge.setDeposit(false);
            }
            bookingChargeRepository.saveAndFlush(charge);
        }
    }

    private void validateRequest(BookingPoolRequest request) {
        if (request == null || (request.customerId() == null) == (request.newCustomer() == null))
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        if (request.pool() == null || request.pool().lines() == null || request.pool().lines().isEmpty())
            throw new ValidationException("Au moins une ligne de formule piscine est requise.");
        if (request.pool().guestsCount() != null && request.pool().guestsCount() < 1)
            throw new ValidationException("Le nombre de personnes doit être supérieur ou égal à 1.");
        if (request.pool().lines().size() > 1 && request.pool().guestsCount() != null) {
            int distributedGuests = request.pool().lines().stream()
                    .mapToInt(BookingPoolRequest.RateItem::quantity)
                    .sum();
            if (distributedGuests != request.pool().guestsCount()) {
                throw new ValidationException("La somme des personnes par tranche doit correspondre au nombre total de personnes.");
            }
        }
        if (request.payment() != null && (request.payment().methodId() == null || request.payment().amount() == null
                || request.payment().amount().signum() <= 0))
            throw new ValidationException("Le moyen et le montant du paiement sont obligatoires.");
    }

    private void validateSlot(boolean exclusive, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (exclusive && (endsAt == null || !endsAt.isAfter(startsAt)))
            throw new ValidationException("Une formule exclusive exige une fin après le début.");
        if (!exclusive && endsAt != null)
            throw new ValidationException("Une formule d'entrée ne doit pas avoir de date de fin.");
    }
}
