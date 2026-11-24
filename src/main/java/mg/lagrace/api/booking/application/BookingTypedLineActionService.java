package mg.lagrace.api.booking.application;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.pool.Pool;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.booking.pool.BookingPool;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingStatusActionRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.booking.pool.persistence.BookingPoolRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class BookingTypedLineActionService {
    @Autowired private BookingPermissionService permissions;
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms;
    private final BookingStatusService statuses;
    private final BookingDetailService details;
    private final UserRepository users;
    private final Clock clock;

    public BookingTypedLineActionService(BookingPoolRepository pools,
            BookingMeetingRoomRepository meetingRooms, BookingStatusService statuses,
            BookingDetailService details, UserRepository users, Clock clock) {
        this.pools = pools;
        this.meetingRooms = meetingRooms;
        this.statuses = statuses;
        this.details = details;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse startPool(Long id, BookingStatusActionRequest request, Authentication authentication) {
        BookingPool line = pools.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable avec l'id : " + id));
        return startPoolLine(line, request, authentication);
    }

    @Transactional
    public BookingDetailResponse endPool(Long id, BookingStatusActionRequest request, Authentication authentication) {
        BookingPool line = pools.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable avec l'id : " + id));
        return transition(line, line.getBooking(), line.getVersion(), id, request, authentication, "CHECK_OUT");
    }

    @Transactional
    public BookingDetailResponse startMeetingRoom(Long id, BookingStatusActionRequest request, Authentication authentication) {
        BookingMeetingRoom line = meetingRooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne salle de réunion introuvable avec l'id : " + id));
        return startMeetingLine(line, request, authentication);
    }

    @Transactional
    public BookingDetailResponse endMeetingRoom(Long id, BookingStatusActionRequest request, Authentication authentication) {
        BookingMeetingRoom line = meetingRooms.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne salle de réunion introuvable avec l'id : " + id));
        return transition(line, line.getBooking(), line.getVersion(), id, request, authentication, "CHECK_OUT");
    }

    private BookingDetailResponse startPoolLine(BookingPool line, BookingStatusActionRequest request, Authentication authentication) {
        Booking booking = line.getBooking();
        User user = validateStart(booking, line.getVersion(), request, authentication);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean exclusive = line.getPoolService() != null && Boolean.TRUE.equals(line.getPoolService().getIsExculusive());
        boolean scheduled = exclusive
                ? line.getStartsAt() != null && line.getEndsAt() != null && !now.isBefore(line.getStartsAt().minusMinutes(30)) && !now.isAfter(line.getEndsAt())
                : line.getStartsAt() != null && LocalDate.now(clock.withZone(MADAGASCAR)).equals(line.getStartsAt().atZoneSameInstant(MADAGASCAR).toLocalDate());
        requireScheduledOrAdministrator(scheduled, authentication, "Le début de la réservation piscine est hors du créneau prévu.");
        String reason = cleanReason(request.reason());
        statuses.changeLineStatus(line, "CHECK_IN", reason, user.getIdUser(), now);
        promoteBooking(booking, reason, user, now);
        return details.get(booking.getIdBooking());
    }

    private BookingDetailResponse startMeetingLine(BookingMeetingRoom line, BookingStatusActionRequest request, Authentication authentication) {
        Booking booking = line.getBooking();
        User user = validateStart(booking, line.getVersion(), request, authentication);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean scheduled = LocalDate.now(clock.withZone(MADAGASCAR)).equals(line.getServiceDate());
        requireScheduledOrAdministrator(scheduled, authentication, "Le début de la salle de réunion n'est pas prévu aujourd'hui.");
        String reason = cleanReason(request.reason());
        statuses.changeLineStatus(line, "CHECK_IN", reason, user.getIdUser(), now);
        promoteBooking(booking, reason, user, now);
        return details.get(booking.getIdBooking());
    }

    private User validateStart(Booking booking, Integer version, BookingStatusActionRequest request, Authentication authentication) {
        statuses.assertBookingOpen(booking);
        if (request == null || request.version() == null || !request.version().equals(version)) throw new ConflictException("STALE_VERSION", "La ligne a été modifiée par quelqu'un d'autre.");
        if (booking == null || booking.getStatus() == null || !Set.of("PENDING", "CONFIRMED", "IN_PROGRESS").contains(booking.getStatus().getCode())) throw new ConflictException("FORBIDDEN_TRANSITION", "Le dossier doit être en attente, confirmé ou en cours.");
        if (authentication == null || authentication.getName() == null) throw new ResourceNotFoundException("Utilisateur authentifié introuvable.");
        return users.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private void promoteBooking(Booking booking, String reason, User user, OffsetDateTime now) {
        if (!"IN_PROGRESS".equals(booking.getStatus().getCode())) statuses.changeBookingStatus(booking, "IN_PROGRESS", reason, user.getIdUser(), now);
    }

    private void requireScheduledOrAdministrator(boolean scheduled, Authentication authentication, String message) {
        if (!scheduled && !permissions.isAuthorized(authentication, "BOOKING_UPDATE"))
            throw new BusinessException("FORBIDDEN", message + " La permission BOOKING_UPDATE est requise.");
    }

    private String cleanReason(String reason) { return reason == null || reason.isBlank() ? null : reason.trim(); }

    private BookingDetailResponse transition(Object line, Booking booking, Integer version,
            Long id, BookingStatusActionRequest request, Authentication authentication, String status) {
        statuses.assertBookingOpen(booking);
        if (request == null || request.version() == null || !request.version().equals(version)) {
            throw new ConflictException("STALE_VERSION", "La ligne a été modifiée par quelqu'un d'autre.");
        }
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Utilisateur authentifié introuvable.");
        }
        User user = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        statuses.changeLineStatus(line, status, reason, user.getIdUser(), OffsetDateTime.now(clock));
        return details.get(booking.getIdBooking());
    }
}
