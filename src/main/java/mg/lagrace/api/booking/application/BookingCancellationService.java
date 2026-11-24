package mg.lagrace.api.booking.application;

import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.booking.pool.BookingPool;
import mg.lagrace.api.booking.room.BookingRoom;
import mg.lagrace.api.booking.space.BookingSpace;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingCancelRequest;
import mg.lagrace.api.booking.dto.BookingCancellationFee;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingRoomCancelRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.booking.pool.persistence.BookingPoolRepository;
import mg.lagrace.api.booking.room.persistence.BookingRoomRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class BookingCancellationService {
    private static final String CANCEL_REASON = "Dossier annulé";
    private static final String SPACE_EVENT_CANCEL_REASON = "Événement annulé";

    private final BookingRepository bookings;
    private final BookingRoomRepository rooms;
    private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms;
    private final BookingSpaceRepository spaces;
    private final BookingChargeRepository charges;
    private final BookingOtherRepository others;
    private final BookingBalanceRepository balances;
    private final UserRepository users;
    private final BookingStatusService statuses;
    private final BookingChangeLogger logger;
    private final BookingDetailService details;
    private final Clock clock;

    public BookingCancellationService(BookingRepository bookings, BookingRoomRepository rooms,
            BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms, BookingSpaceRepository spaces,
            BookingChargeRepository charges, BookingOtherRepository others, BookingBalanceRepository balances,
            UserRepository users, BookingStatusService statuses, BookingChangeLogger logger,
            BookingDetailService details, Clock clock) {
        this.bookings = bookings; this.rooms = rooms; this.pools = pools; this.meetingRooms = meetingRooms;
        this.spaces = spaces; this.charges = charges; this.others = others; this.balances = balances;
        this.users = users; this.statuses = statuses; this.logger = logger; this.details = details; this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse cancelRoom(Long id, BookingRoomCancelRequest request,
            Authentication authentication) {
        BookingRoom line = rooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + id));
        return cancelLine(line.getBooking(), line.getVersion(), code(line.getStatus()), request, authentication, false, null,
                (reason, userId) -> statuses.changeLineStatus(line, "CANCELED", reason, userId));
    }

    @Transactional
    public BookingDetailResponse cancelSpace(Long id, BookingRoomCancelRequest request,
            Authentication authentication) {
        BookingSpace line = spaces.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne espace introuvable avec l'id : " + id));
        return cancelLine(line.getBooking(), line.getVersion(), code(line.getStatus()), request, authentication, true, null,
                (reason, userId) -> statuses.changeLineStatus(line, "CANCELED", reason, userId));
    }

        @Transactional
        public BookingDetailResponse cancelPool(Long id, BookingRoomCancelRequest request,
            Authentication authentication) {
        BookingPool line = pools.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable avec l'id : " + id));
        return cancelLine(line.getBooking(), line.getVersion(), code(line.getStatus()), request, authentication, false, "POOL",
            (reason, userId) -> statuses.changeLineStatus(line, "CANCELED", reason, userId));
        }

        @Transactional
        public BookingDetailResponse cancelMeetingRoom(Long id, BookingRoomCancelRequest request,
            Authentication authentication) {
        BookingMeetingRoom line = meetingRooms.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ligne salle de réunion introuvable avec l'id : " + id));
        return cancelLine(line.getBooking(), line.getVersion(), code(line.getStatus()), request, authentication, false, "MEETING_ROOM",
            (reason, userId) -> statuses.changeLineStatus(line, "CANCELED", reason, userId));
        }

    private BookingDetailResponse cancelLine(Booking booking, Integer version, String lineStatus,
            BookingRoomCancelRequest request, Authentication authentication, boolean spaceLine, String domainScope,
            BiConsumer<String, Long> cancel) {
        booking = openBooking(booking);
        User user = requireUser(authentication);
        requireReason(request == null ? null : request.reason());
        if (request == null || request.version() == null || !request.version().equals(version))
            throw new ConflictException("STALE_VERSION", "La ligne a été modifiée par quelqu'un d'autre.");
        if (!"CONFIRMED".equals(lineStatus))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être annulée.");

        addFee(booking, request.cancellationFee(), user, request.reason());
        if (spaceLine) voidSpaceEventCharges(booking, user, request.reason());
        if (domainScope != null && isLastActiveDomainLine(booking.getIdBooking(), domainScope))
            voidDomainServiceCharges(booking, user, request.reason(), domainScope);
        cancel.accept(request.reason().trim(), user.getIdUser());
        return cancellationResponse(booking, !hasActiveLine(booking.getIdBooking()), spaceLine);
    }

    @Transactional
    public BookingDetailResponse cancelBooking(Long id, BookingCancelRequest request,
            Authentication authentication) {
        Booking booking = bookings.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
        User user = requireUser(authentication);
        requireReason(request == null ? null : request.reason());
        if (request == null || request.version() == null || !request.version().equals(booking.getVersion()))
            throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");
        if (!Set.of("PENDING", "CONFIRMED", "IN_PROGRESS").contains(code(booking.getStatus())))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Le dossier ne peut pas être annulé dans son état actuel.");
        if (hasLineStatus(booking.getIdBooking(), Set.of("CHECK_IN", "CHECK_OUT")))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Impossible d'annuler un dossier contenant une ligne arrivée ou départée.");

        voidCharges(booking, user, request.reason());
        voidOthers(booking, user, request.reason());
        addFee(booking, request.cancellationFee(), user, request.reason());
        cancelActiveLines(booking, user, request.reason());
        statuses.changeBookingStatus(booking, "CANCELED", request.reason().trim(), user.getIdUser());
        return cancellationResponse(booking, true, true);
    }

    private Booking openBooking(Booking booking) {
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(code(booking.getStatus())))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        return booking;
    }

    private void voidCharges(Booking booking, User user, String reason) {
        for (BookingCharge charge : charges.findAllByBooking_IdBooking(booking.getIdBooking())) {
            if (charge.getVoidedAt() != null) continue;
            OffsetDateTime now = OffsetDateTime.now(clock);
            charge.setVoidedAt(now); charge.setVoidReason(CANCEL_REASON); charges.saveAndFlush(charge);
            logger.log(booking.getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "void_reason", null,
                    CANCEL_REASON, reason, user.getIdUser(), UUID.randomUUID());
        }
    }

    private void voidOthers(Booking booking, User user, String reason) {
        for (BookingOther other : others.findAllByBooking_IdBookingOrderByCreatedAtDesc(booking.getIdBooking())) {
            if (other.getVoidedAt() != null || Boolean.TRUE.equals(other.getIsCancellationFee())) continue;
            OffsetDateTime now = OffsetDateTime.now(clock);
            other.setVoidedAt(now); other.setVoidReason(CANCEL_REASON); others.saveAndFlush(other);
            logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "VOID", "void_reason", null,
                    CANCEL_REASON, reason, user.getIdUser(), UUID.randomUUID());
        }
    }


    private void voidSpaceEventCharges(Booking booking, User user, String reason) {
        for (BookingCharge charge : charges.findAllByBooking_IdBooking(booking.getIdBooking())) {
            if (charge.getVoidedAt() != null || !isSpaceEventCharge(charge)) continue;
            OffsetDateTime now = OffsetDateTime.now(clock);
            charge.setVoidedAt(now);
            charge.setVoidReason(SPACE_EVENT_CANCEL_REASON);
            charges.saveAndFlush(charge);
            UUID operationId = UUID.randomUUID();
            logger.log(booking.getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "voided_at", null,
                    now, reason, user.getIdUser(), operationId);
            logger.log(booking.getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "void_reason", null,
                    SPACE_EVENT_CANCEL_REASON, reason, user.getIdUser(), operationId);
        }
    }

    /** Restaurant charges deliberately have no additional-service scope and remain billable. */
    private void voidDomainServiceCharges(Booking booking, User user, String reason, String domainScope) {
        for (BookingCharge charge : charges.findAllByBooking_IdBooking(booking.getIdBooking())) {
            if (charge.getVoidedAt() != null || charge.getAdditionalService() == null
                    || charge.getAdditionalService().getScope() == null) continue;
            String scope = charge.getAdditionalService().getScope().getCode();
            if (!domainScope.equalsIgnoreCase(scope)) continue;
            OffsetDateTime now = OffsetDateTime.now(clock);
            charge.setVoidedAt(now); charge.setVoidReason("Réservation annulée");
            charges.saveAndFlush(charge);
            UUID operationId = UUID.randomUUID();
            logger.log(booking.getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "voided_at", null,
                    now, reason, user.getIdUser(), operationId);
            logger.log(booking.getIdBooking(), "CHARGE", charge.getIdBookingCharge(), "VOID", "void_reason", null,
                    "Réservation annulée", reason, user.getIdUser(), operationId);
        }
    }

    private boolean isLastActiveDomainLine(Long bookingId, String domainScope) {
        return switch (domainScope) {
            case "POOL" -> pools.findAllByBooking_IdBooking(bookingId).stream().filter(line -> Boolean.TRUE.equals(line.getActive())).count() <= 1;
            case "MEETING_ROOM" -> meetingRooms.findAllByBooking_IdBooking(bookingId).stream().filter(line -> Boolean.TRUE.equals(line.getActive())).count() <= 1;
            default -> false;
        };
    }

    private boolean isSpaceEventCharge(BookingCharge charge) {
        if (Boolean.TRUE.equals(charge.getDeposit())) return false;
        if (charge.getBanquetRate() != null) return true;
        if (charge.getAdditionalService() == null || charge.getAdditionalService().getScope() == null) return false;
        String scope = charge.getAdditionalService().getScope().getCode();
        return "SPACE".equalsIgnoreCase(scope) || "BANQUET".equalsIgnoreCase(scope);
    }
    private void addFee(Booking booking, BookingCancellationFee fee, User user, String reason) {
        if (fee == null) return;
        if (fee.label() == null || fee.label().isBlank() || fee.amount() == null || fee.amount().signum() <= 0)
            throw new ValidationException("Le libellé et le montant des frais d'annulation sont obligatoires.");
        BookingOther other = new BookingOther();
        other.setBooking(booking); other.setLabel(fee.label().trim()); other.setAmount(fee.amount());
        other.setIsCancellationFee(true);
        other.setCreatedByUser(user); OffsetDateTime now = OffsetDateTime.now(clock);
        other.setCreatedAt(now); other.setUpdatedAt(now);
        other = others.saveAndFlush(other);
        logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "CREATE", null, null,
                fee.label().trim() + " : " + fee.amount(), reason, user.getIdUser(), UUID.randomUUID());
    }

    private void cancelActiveLines(Booking booking, User user, String reason) {
        for (BookingRoom line : rooms.findAllByBooking_IdBooking(booking.getIdBooking()))
            if (Boolean.TRUE.equals(line.getActive())) statuses.changeLineStatus(line, "CANCELED", reason.trim(), user.getIdUser());
        for (BookingPool line : pools.findAllByBooking_IdBooking(booking.getIdBooking()))
            if (Boolean.TRUE.equals(line.getActive())) statuses.changeLineStatus(line, "CANCELED", reason.trim(), user.getIdUser());
        for (BookingMeetingRoom line : meetingRooms.findAllByBooking_IdBooking(booking.getIdBooking()))
            if (Boolean.TRUE.equals(line.getActive())) statuses.changeLineStatus(line, "CANCELED", reason.trim(), user.getIdUser());
        for (BookingSpace line : spaces.findAllByBooking_IdBooking(booking.getIdBooking()))
            if (Boolean.TRUE.equals(line.getActive())) statuses.changeLineStatus(line, "CANCELED", reason.trim(), user.getIdUser());
    }

    private boolean hasActiveLine(Long bookingId) {
        return rooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || pools.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || meetingRooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()))
                || spaces.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
    }

    private boolean hasLineStatus(Long bookingId, Set<String> statusesToFind) {
        return rooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || pools.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || meetingRooms.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || spaces.findAllByBooking_IdBooking(bookingId).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())));
    }

    private BookingDetailResponse cancellationResponse(Booking booking, boolean noActiveLine, boolean includeDepositToReturn) {
        BookingBalance balance = balances.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        BigDecimal refundDue = n(balance.getPaid()).subtract(n(balance.getTotal())).max(BigDecimal.ZERO);
        BigDecimal depositToReturn = n(balance.getDepositHeld());
        return details.get(booking.getIdBooking()).withCancellationSummary(noActiveLine, refundDue,
            includeDepositToReturn ? depositToReturn : null);
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        return users.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new ValidationException("Le motif d'annulation est obligatoire.");
    }

    private String code(BookingLineStatus status) { return status == null ? null : status.getCode(); }
    private String code(mg.lagrace.api.booking.core.BookingStatus status) { return status == null ? null : status.getCode(); }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
