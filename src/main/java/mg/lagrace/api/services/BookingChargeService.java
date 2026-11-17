package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.booking.BookingBanquetCreateRequest;
import mg.lagrace.api.dto.booking.*;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.*;
import mg.lagrace.api.repositories.*;

@Service
public class BookingChargeService {
    private final BookingRepository bookings; private final BookingChargeRepository charges;
    private final MenuItemRepository menuItems; private final MenuItemRateRepository menuRates;
    private final AdditionalServiceRepository additionalServices; private final BookingRoomRepository rooms;
    private final BookingSpaceRepository spaces; private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms; private final BanquetRateRepository banquetRates;
    private final BanquetMenuTypeRepository menuTypes;
    private final BookingBalanceRepository balances; private final UserRepository users;
    private final BookingStatusService statusService; private final BookingChangeLogger logger;
    private final BookingDetailService details; private final Clock clock;

    public BookingChargeService(BookingRepository bookings, BookingChargeRepository charges, MenuItemRepository menuItems,
            MenuItemRateRepository menuRates, AdditionalServiceRepository additionalServices, BookingRoomRepository rooms,
            BookingSpaceRepository spaces, BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms,
            BanquetRateRepository banquetRates, BanquetMenuTypeRepository menuTypes,
            BookingBalanceRepository balances, UserRepository users, BookingStatusService statusService,
            BookingChangeLogger logger, BookingDetailService details, Clock clock) {
        this.bookings = bookings; this.charges = charges; this.menuItems = menuItems; this.menuRates = menuRates;
        this.additionalServices = additionalServices; this.rooms = rooms; this.spaces = spaces; this.pools = pools;
        this.meetingRooms = meetingRooms; this.banquetRates = banquetRates; this.menuTypes = menuTypes;
        this.balances = balances; this.users = users;
        this.statusService = statusService; this.logger = logger; this.details = details; this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse add(Long bookingId, BookingChargeCreateRequest request, Authentication authentication) {
        Booking booking = openBooking(bookingId); requireUser(authentication);
        if (request == null || !request.hasSingleCatalogSource() || request.quantity() == null || request.quantity() < 1)
            throw new ValidationException("Indiquez exactement un article catalogue et une quantité positive.");
        User user = user(authentication);
        BookingCharge charge = new BookingCharge(); charge.setBooking(booking); charge.setQuantity(request.quantity()); charge.setIncluded(false);
        charge.setCreatedByUser(user);
        boolean forceFree = false;
        if (request.menuItemId() != null) {
            MenuItem item = menuItems.findById(request.menuItemId()).orElseThrow(() -> new ResourceNotFoundException("Article de restaurant introuvable."));
            if (!Boolean.TRUE.equals(item.getActive())) throw new UnprocessableEntityException("Cet article n'est plus actif.");
            MenuItemRate rate = menuRates.findApplicableByMenuItem(item.getIdMenuItem(), chargeDate(booking));
            if (rate == null) throw new UnprocessableEntityException("Aucun tarif applicable pour cet article.");
            charge.setMenuItem(item); charge.setLabel(item.getName()); charge.setUnitPrice(rate.getAmount()); charge.setDeposit(false);
        } else {
            AdditionalService item = additionalServices.findById(request.additionalServiceId()).orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
            if (!item.isActive()) throw new UnprocessableEntityException("Ce service n'est plus actif.");
            if (item.isDeposit()) throw new ValidationException("Une caution ne peut pas être ajoutée comme service. Utilisez l'opération de caution.");
            validateScope(booking, item);
            validateCateringCondition(booking, item);
            charge.setAdditionalService(item); charge.setLabel(item.getName()); charge.setUnitPrice(item.getPrice()); charge.setDeposit(item.isDeposit());
            forceFree = isFreeWithCaterer(booking, item);
        }
        if (Boolean.TRUE.equals(request.included()) && !forceFree && !canOffer(authentication))
            throw new BusinessException("FORBIDDEN", "Seul un rôle autorisé peut offrir une charge.");
        charge.setIncluded(Boolean.TRUE.equals(request.included()) || forceFree);
        charge = charges.saveAndFlush(charge);
        UUID operationId = UUID.randomUUID();
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", null, null,
                charge.getLabel(), null, user.getIdUser(), operationId);
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", "quantity", null,
                charge.getQuantity(), null, user.getIdUser(), operationId);
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", "included", null,
                charge.getIncluded(), null, user.getIdUser(), operationId);
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse addBanquet(Long bookingId, BookingBanquetCreateRequest request,
            Authentication authentication) {
        Booking booking = openBookingForUpdate(bookingId);
        User user = requireUser(authentication);
        if (request == null || request.menuTypeId() == null || request.persons() == null || request.persons() < 1)
            throw new ValidationException("Le type de menu et un nombre de personnes positif sont obligatoires.");
        BookingSpace space = activeCateredSpace(bookingId);
        if (space == null) throw catererRule("Un menu banquet exige une réservation d'espace active avec traiteur.");
        if (request.persons() > space.getGuestsCount())
            throw new ValidationException("Le nombre de personnes du menu ne peut pas dépasser le nombre d'invités.", "persons");
        boolean alreadyAdded = charges.findAllByBooking_IdBooking(bookingId).stream()
                .anyMatch(charge -> charge.getVoidedAt() == null && charge.getBanquetRate() != null
                        && request.menuTypeId().equals(charge.getBanquetRate().getMenuType().getIdMenuType()));
        if (alreadyAdded) throw new UnprocessableEntityException("BANQUET_DUPLICATE", "Ce type de menu est déjà présent au banquet.");
        BanquetMenuType menuType = menuTypes.findById(request.menuTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de menu banquet introuvable."));
        BanquetRate rate = banquetRate(menuType.getIdMenuType(), space.getEventDate(), request.persons());
        BookingCharge charge = new BookingCharge();
        charge.setBooking(booking);
        charge.setBanquetRate(rate);
        charge.setLabel(menuType.getName());
        charge.setQuantity(request.persons());
        charge.setUnitPrice(rate.getAmount());
        charge.setIncluded(false);
        charge.setDeposit(false);
        charge.setCreatedByUser(user);
        charge = charges.saveAndFlush(charge);
        UUID operationId = UUID.randomUUID();
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", null, null,
                charge.getLabel(), null, user.getIdUser(), operationId);
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", "quantity", null,
                charge.getQuantity(), null, user.getIdUser(), operationId);
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", "included", null,
                false, null, user.getIdUser(), operationId);
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse update(Long id, BookingChargeUpdateRequest request, Authentication authentication) {
        BookingCharge charge = charge(id); Booking booking = openBookingForUpdate(charge.getBooking().getIdBooking()); User user = requireUser(authentication);
        if (request == null || !request.has("version") || !request.getVersion().equals(charge.getVersion())) throw new ConflictException("STALE_VERSION", "La charge a été modifiée par quelqu'un d'autre.");
        if (!request.has("quantity") && !request.has("persons") && !request.has("included")) throw new ValidationException("Au moins persons, quantity ou included est obligatoire.");
        if (request.has("persons") && charge.getBanquetRate() == null)
            throw new ValidationException("persons ne s'applique qu'à une charge banquet.");
        if (request.has("persons") && request.has("quantity"))
            throw new ValidationException("Utilisez persons ou quantity, pas les deux.");
        if (Boolean.TRUE.equals(charge.getDeposit()) && request.has("included")
                && Boolean.TRUE.equals(request.getIncluded()))
            throw new ValidationException("Une caution ne peut pas être offerte.");
        if (Boolean.TRUE.equals(charge.getDeposit()) && request.has("quantity"))
            throw new ValidationException("Le montant fixe d'une caution ne peut pas être modifié.");
        boolean forcedFree = charge.getAdditionalService() != null
                && isFreeWithCaterer(booking, charge.getAdditionalService());
        if (request.has("included") && Boolean.TRUE.equals(request.getIncluded()) && !forcedFree && !canOffer(authentication))
            throw new BusinessException("FORBIDDEN", "Seul un rôle autorisé peut offrir une charge.");
        if (request.has("included") && Boolean.FALSE.equals(request.getIncluded()) && forcedFree && !canOffer(authentication))
            throw new BusinessException("FORBIDDEN", "Un service offert avec traiteur ne peut pas être repassé en payant sans autorisation.");
        UUID operationId = UUID.randomUUID();
        if (request.has("quantity") || request.has("persons")) {
            Integer quantity = request.has("persons") ? request.getPersons() : request.getQuantity();
            if (quantity == null || quantity < 0) throw new ValidationException("La quantité doit être positive ou nulle.");
            if (request.has("persons") && quantity == 0) throw new ValidationException("Le nombre de personnes doit être positif.", "persons");
            if (quantity == 0) { voidCharge(charge, request.getReason(), user, operationId); return details.get(booking.getIdBooking()); }
            if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Une charge annulée ne peut pas être réactivée.");
            if (charge.getBanquetRate() != null) {
                BookingSpace space = activeCateredSpace(booking.getIdBooking());
                if (space == null) throw catererRule("Un menu banquet exige une réservation d'espace active avec traiteur.");
                if (quantity > space.getGuestsCount()) throw new ValidationException("Le nombre de personnes du menu ne peut pas dépasser le nombre d'invités.", "persons");
                BanquetRate currentRate = charge.getBanquetRate();
                BanquetRate newRate = banquetRate(currentRate.getMenuType().getIdMenuType(), space.getEventDate(), quantity);
                if (!newRate.getIdRate().equals(currentRate.getIdRate())) {
                    charge.setBanquetRate(newRate);
                    charge.setUnitPrice(newRate.getAmount());
                    logger.log(booking.getIdBooking(), "CHARGE", id, "UPDATE", "unit_price", currentRate.getAmount(),
                            newRate.getAmount(), request.getReason(), user.getIdUser(), operationId);
                }
            }
            Integer old = charge.getQuantity(); charge.setQuantity(quantity);
            logger.log(booking.getIdBooking(), "CHARGE", id, "UPDATE", "quantity", old, quantity, request.getReason(), user.getIdUser(), operationId);
        }
        if (request.has("included")) {
            if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Une charge annulée ne peut pas être modifiée.");
            Boolean old = charge.getIncluded(); charge.setIncluded(Boolean.TRUE.equals(request.getIncluded()));
            logger.log(booking.getIdBooking(), "CHARGE", id, "UPDATE", "included", old, charge.getIncluded(), request.getReason(), user.getIdUser(), operationId);
        }
        charges.saveAndFlush(charge);
        return details.get(booking.getIdBooking());
    }

    @Transactional
    public BookingDetailResponse voidCharge(Long id, BookingChargeVoidRequest request, Authentication authentication) {
        BookingCharge charge = charge(id); Booking booking = openBookingForUpdate(charge.getBooking().getIdBooking()); User user = requireUser(authentication);
        if (request == null || request.version() == null || !request.version().equals(charge.getVersion())) throw new ConflictException("STALE_VERSION", "La charge a été modifiée par quelqu'un d'autre.");
        UUID operationId = UUID.randomUUID(); voidCharge(charge, request.reason(), user, operationId);
        return details.get(booking.getIdBooking());
    }

    private void voidCharge(BookingCharge charge, String reason, User user, UUID operationId) {
        if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Cette charge est déjà annulée.");
        if (charge.getBanquetRate() != null && activeCateredSpace(charge.getBooking().getIdBooking()) != null) {
            long activeMenus = charges.findAllByBooking_IdBooking(charge.getBooking().getIdBooking()).stream()
                    .filter(item -> item.getVoidedAt() == null && item.getBanquetRate() != null).count();
            if (activeMenus <= 1) throw catererRule("Le dernier menu d'un dossier avec traiteur ne peut pas être retiré. Changez d'abord le mode traiteur.");
        }
        if (Boolean.TRUE.equals(charge.getDeposit()) && balances.findById(charge.getBooking().getIdBooking()).map(b -> n(b.getDepositHeld()).signum() > 0).orElse(false))
            throw new ConflictException("FORBIDDEN", "Rendez la caution d'abord.");
        OffsetDateTime now = OffsetDateTime.now(clock); charge.setVoidedAt(now); charge.setVoidReason(reason == null || reason.isBlank() ? null : reason.trim()); charges.saveAndFlush(charge);
        logger.log(charge.getBooking().getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "voided_at", null, now, reason, user.getIdUser(), operationId);
        logger.log(charge.getBooking().getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "void_reason", null, charge.getVoidReason(), reason, user.getIdUser(), operationId);
    }
    private Booking openBooking(Long id) { Booking booking = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id)); statusService.assertBookingOpen(booking); return booking; }
    private Booking openBookingForUpdate(Long id) { Booking booking = bookings.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id)); statusService.assertBookingOpen(booking); return booking; }
    private BookingCharge charge(Long id) { return charges.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charge introuvable avec l'id : " + id)); }
    private User requireUser(Authentication authentication) { if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié."); return user(authentication); }
    private User user(Authentication authentication) { return users.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")); }
    private void validateScope(Booking booking, AdditionalService service) {
        String scope = service.getScope() == null ? "" : service.getScope().getCode().toUpperCase();
        boolean hasActiveScope = switch (scope) {
            case "ROOM" -> rooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
            case "POOL" -> pools.findAllByBooking_IdBooking(booking.getIdBooking()).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
            case "MEETING_ROOM" -> meetingRooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
            case "SPACE" -> spaces.findAllByBooking_IdBooking(booking.getIdBooking()).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
            case "BANQUET" -> activeCateredSpace(booking.getIdBooking()) != null;
            case "GENERAL" -> hasAnyActiveLine(booking.getIdBooking());
            default -> false;
        };
        if (!hasActiveScope) throw new UnprocessableEntityException("SERVICE_SCOPE", "Ce service ne correspond à aucune ligne active du dossier.");
    }
    private boolean hasAnyActiveLine(Long bookingId) {
        return rooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || spaces.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || pools.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || meetingRooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
    }
    private BookingSpace activeCateredSpace(Long bookingId) {
        return spaces.findAllByBooking_IdBooking(bookingId).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive()) && Boolean.TRUE.equals(line.getWithCaterer()))
                .findFirst().orElse(null);
    }
    private void validateCateringCondition(Booking booking, AdditionalService service) {
        if (service.getCateringCondition() == null) return;
        BookingSpace space = spaces.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive())).findFirst().orElse(null);
        if (space == null) return;
        boolean withCaterer = Boolean.TRUE.equals(space.getWithCaterer());
        if ((withCaterer && service.getCateringCondition() == mg.lagrace.api.models.enums.CateringCondition.WITHOUT_CATERER)
                || (!withCaterer && service.getCateringCondition() == mg.lagrace.api.models.enums.CateringCondition.WITH_CATERER))
            throw catererRule("Le service " + service.getName() + " n'est pas compatible avec le mode traiteur choisi.");
    }
    private boolean isFreeWithCaterer(Booking booking, AdditionalService service) {
        return service.isFreeWithCaterer() && activeCateredSpace(booking.getIdBooking()) != null;
    }
    private BanquetRate banquetRate(Long menuTypeId, LocalDate eventDate, int persons) {
        List<BanquetRate> matches = banquetRates.findApplicableByMenuType(menuTypeId, eventDate).stream()
                .filter(rate -> persons >= rate.getMinPersons()
                        && (rate.getMaxPersons() == null || persons <= rate.getMaxPersons())).toList();
        if (matches.size() != 1) throw new UnprocessableEntityException("NO_TARIFF", "Aucun palier banquet applicable à cette date et pour ce nombre de personnes.");
        return matches.get(0);
    }
    private LocalDate chargeDate(Booking booking) {
        LocalDate spaceDate = spaces.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive())).map(BookingSpace::getEventDate)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (spaceDate != null) return spaceDate;
        LocalDate poolDate = pools.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive()) && line.getStartsAt() != null)
                .map(line -> line.getStartsAt().toInstant().atZone(java.time.ZoneId.of("Indian/Antananarivo")).toLocalDate())
                .findFirst().orElse(null);
        if (poolDate != null) return poolDate;
        LocalDate meetingDate = meetingRooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive())).map(BookingMeetingRoom::getServiceDate)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (meetingDate != null) return meetingDate;
        return rooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive())).map(BookingRoom::getCheckIn)
                .filter(java.util.Objects::nonNull).findFirst().orElse(LocalDate.now(clock));
    }
    private mg.lagrace.api.exceptions.UnprocessableEntityException catererRule(String message) {
        return new UnprocessableEntityException("CATERER_RULE", message, "space.withCaterer");
    }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean canOffer(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_Administrateur".equals(authority.getAuthority()));
    }
}
