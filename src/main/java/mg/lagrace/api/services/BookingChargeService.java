package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.booking.*;
import mg.lagrace.api.exceptions.ConflictException;
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
    private final BookingBalanceRepository balances; private final UserRepository users;
    private final BookingStatusService statusService; private final BookingChangeLogger logger;
    private final BookingDetailService details; private final Clock clock;

    public BookingChargeService(BookingRepository bookings, BookingChargeRepository charges, MenuItemRepository menuItems,
            MenuItemRateRepository menuRates, AdditionalServiceRepository additionalServices, BookingRoomRepository rooms,
            BookingBalanceRepository balances, UserRepository users, BookingStatusService statusService,
            BookingChangeLogger logger, BookingDetailService details, Clock clock) {
        this.bookings = bookings; this.charges = charges; this.menuItems = menuItems; this.menuRates = menuRates;
        this.additionalServices = additionalServices; this.rooms = rooms; this.balances = balances; this.users = users;
        this.statusService = statusService; this.logger = logger; this.details = details; this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse add(Long bookingId, BookingChargeCreateRequest request, Authentication authentication) {
        Booking booking = openBooking(bookingId); requireUser(authentication);
        if (request == null || !request.hasSingleCatalogSource() || request.quantity() == null || request.quantity() < 1)
            throw new ValidationException("Indiquez exactement un article catalogue et une quantité positive.");
        BookingCharge charge = new BookingCharge(); charge.setBooking(booking); charge.setQuantity(request.quantity()); charge.setIncluded(false);
        if (request.menuItemId() != null) {
            MenuItem item = menuItems.findById(request.menuItemId()).orElseThrow(() -> new ResourceNotFoundException("Article de restaurant introuvable."));
            if (!Boolean.TRUE.equals(item.getActive())) throw new UnprocessableEntityException("Cet article n'est plus actif.");
            MenuItemRate rate = menuRates.findApplicableByMenuItem(item.getIdMenuItem(), LocalDate.now(clock));
            if (rate == null) throw new UnprocessableEntityException("Aucun tarif applicable pour cet article.");
            charge.setMenuItem(item); charge.setLabel(item.getName()); charge.setUnitPrice(rate.getAmount()); charge.setDeposit(false);
        } else {
            AdditionalService item = additionalServices.findById(request.additionalServiceId()).orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
            if (!item.isActive()) throw new UnprocessableEntityException("Ce service n'est plus actif.");
            validateRoomScope(booking, item);
            charge.setAdditionalService(item); charge.setLabel(item.getName()); charge.setUnitPrice(item.getPrice()); charge.setDeposit(item.isDeposit());
        }
        charge = charges.saveAndFlush(charge);
        User user = user(authentication); UUID operationId = UUID.randomUUID();
        logger.log(bookingId, "CHARGE", charge.getIdBookingCharge(), "CREATE", null, null,
                charge.getLabel() + " × " + charge.getQuantity(), null, user == null ? null : user.getIdUser(), operationId);
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse update(Long id, BookingChargeUpdateRequest request, Authentication authentication) {
        BookingCharge charge = charge(id); Booking booking = openBooking(charge.getBooking().getIdBooking()); User user = requireUser(authentication);
        if (request == null || !request.has("version") || !request.getVersion().equals(charge.getVersion())) throw new ConflictException("STALE_VERSION", "La charge a été modifiée par quelqu'un d'autre.");
        if (!request.has("quantity") && !request.has("included")) throw new ValidationException("Au moins quantity ou included est obligatoire.");
        UUID operationId = UUID.randomUUID();
        if (request.has("quantity")) {
            Integer quantity = request.getQuantity();
            if (quantity == null || quantity < 0) throw new ValidationException("La quantité doit être positive ou nulle.");
            if (quantity == 0) { voidCharge(charge, request.getReason(), user, operationId); return details.get(booking.getIdBooking()); }
            if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Une charge annulée ne peut pas être réactivée.");
            Integer old = charge.getQuantity(); charge.setQuantity(quantity);
            logger.log(booking.getIdBooking(), "CHARGE", id, "UPDATE", "quantity", old, quantity, null, user.getIdUser(), operationId);
        }
        if (request.has("included")) {
            if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Une charge annulée ne peut pas être modifiée.");
            Boolean old = charge.getIncluded(); charge.setIncluded(Boolean.TRUE.equals(request.getIncluded()));
            logger.log(booking.getIdBooking(), "CHARGE", id, "UPDATE", "included", old, charge.getIncluded(), null, user.getIdUser(), operationId);
        }
        charges.saveAndFlush(charge);
        return details.get(booking.getIdBooking());
    }

    @Transactional
    public BookingDetailResponse voidCharge(Long id, BookingChargeVoidRequest request, Authentication authentication) {
        BookingCharge charge = charge(id); Booking booking = openBooking(charge.getBooking().getIdBooking()); User user = requireUser(authentication);
        if (request == null || request.version() == null || !request.version().equals(charge.getVersion())) throw new ConflictException("STALE_VERSION", "La charge a été modifiée par quelqu'un d'autre.");
        UUID operationId = UUID.randomUUID(); voidCharge(charge, request.reason(), user, operationId);
        return details.get(booking.getIdBooking());
    }

    private void voidCharge(BookingCharge charge, String reason, User user, UUID operationId) {
        if (charge.getVoidedAt() != null) throw new UnprocessableEntityException("Cette charge est déjà annulée.");
        if (Boolean.TRUE.equals(charge.getDeposit()) && balances.findById(charge.getBooking().getIdBooking()).map(b -> n(b.getDepositHeld()).signum() > 0).orElse(false))
            throw new ConflictException("FORBIDDEN", "Rendez la caution d'abord.");
        OffsetDateTime now = OffsetDateTime.now(clock); charge.setVoidedAt(now); charge.setVoidReason(reason == null || reason.isBlank() ? null : reason.trim()); charges.saveAndFlush(charge);
        logger.log(charge.getBooking().getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "voided_at", null, now, reason, user.getIdUser(), operationId);
        logger.log(charge.getBooking().getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "void_reason", null, charge.getVoidReason(), reason, user.getIdUser(), operationId);
    }
    private Booking openBooking(Long id) { Booking booking = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id)); statusService.assertBookingOpen(booking); return booking; }
    private BookingCharge charge(Long id) { return charges.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charge introuvable avec l'id : " + id)); }
    private User requireUser(Authentication authentication) { if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié."); return user(authentication); }
    private User user(Authentication authentication) { return users.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable.")); }
    private void validateRoomScope(Booking booking, AdditionalService service) { if (rooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream().anyMatch(room -> Boolean.TRUE.equals(room.getActive())) && !Set.of("ROOM", "GENERAL").contains(service.getScope().getCode().toUpperCase())) throw new UnprocessableEntityException("Ce service n'est pas compatible avec une réservation de chambre."); }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
