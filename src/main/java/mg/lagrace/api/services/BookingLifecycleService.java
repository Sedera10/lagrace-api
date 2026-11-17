package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.catalog.room.RoomStatus;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingLineStatus;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.views.BookingBalance;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.BookingMeetingRoomRepository;
import mg.lagrace.api.repositories.BookingPoolRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingRoomRepository;
import mg.lagrace.api.repositories.BookingSpaceRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class BookingLifecycleService {
    private final BookingRepository bookings;
    private final BookingBalanceRepository balances;
    private final BookingRoomRepository rooms;
    private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms;
    private final BookingSpaceRepository spaces;
    private final UserRepository users;
    private final BookingStatusService statuses;
    private final BookingDetailService details;
    private final RoomStatusRepository roomStatuses;
    private final RoomService roomService;

    public BookingLifecycleService(BookingRepository bookings, BookingBalanceRepository balances,
            BookingRoomRepository rooms, BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms,
            BookingSpaceRepository spaces, UserRepository users, BookingStatusService statuses,
            BookingDetailService details, RoomStatusRepository roomStatuses, RoomService roomService) {
        this.bookings = bookings; this.balances = balances; this.rooms = rooms; this.pools = pools;
        this.meetingRooms = meetingRooms; this.spaces = spaces; this.users = users;
        this.statuses = statuses; this.details = details;
        this.roomStatuses = roomStatuses; this.roomService = roomService;
    }

    @Transactional
    public BookingDetailResponse confirm(Long id, BookingStatusActionRequest request,
            Authentication authentication) {
        Booking booking = booking(id);
        User user = user(authentication);
        requireVersion(booking, request);
        if (!"PENDING".equals(code(booking)))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seul un dossier En attente peut être confirmé.");
        statuses.changeBookingStatus(booking, "CONFIRMED", null, user.getIdUser());
        return details.get(id);
    }

    @Transactional
    public BookingDetailResponse close(Long id, BookingStatusActionRequest request,
            Authentication authentication) {
        Booking booking = bookings.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
        User user = user(authentication);
        requireVersion(booking, request);
        if (!"IN_PROGRESS".equals(code(booking)))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seul un dossier En cours peut être clôturé.");
        if (hasLineStatus(id, Set.of("CONFIRMED", "CHECK_IN")))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Impossible de clôturer : une ligne est encore CONFIRMED ou CHECK_IN.");

        BookingBalance balance = balances.findById(id)
                .orElseThrow(() -> new IllegalStateException("Solde introuvable dans v_booking_balance."));
        BigDecimal remaining = n(balance.getBalance());
        BigDecimal depositHeld = n(balance.getDepositHeld());
        if (remaining.signum() != 0)
            throw new ConflictException("FORBIDDEN_TRANSITION", "Impossible de clôturer : le solde doit être nul (" + remaining + " Ar).");
        if (depositHeld.signum() != 0)
            throw new ConflictException("FORBIDDEN_TRANSITION", "Impossible de clôturer : la caution détenue doit être nulle (" + depositHeld + " Ar).");

        statuses.changeBookingStatus(booking, "CLOSED", null, user.getIdUser());
        releaseCheckedOutRooms(id);
        return details.get(id);
    }

    private void releaseCheckedOutRooms(Long bookingId) {
        RoomStatus available = roomStatuses.findAll().stream()
                .filter(status -> isNamed(status, "DISPONIBLE"))
                .findFirst().orElse(null);
        if (available == null) return;
        rooms.findAllByBooking_IdBooking(bookingId).stream()
                .filter(line -> "CHECK_OUT".equals(code(line.getStatus())))
                .filter(line -> line.getRoom() != null && isOccupied(line.getRoom().getStatus()))
                .filter(line -> !Boolean.TRUE.equals(
                        rooms.existsOtherCheckIn(line.getRoom().getIdRoom(), line.getIdBookingRoom())))
                .forEach(line -> roomService.changeRoomStatus(line.getRoom().getIdRoom(), available.getIdStatus()));
    }

    private boolean isOccupied(RoomStatus status) {
        return isNamed(status, "OCCUPEE");
    }

    private boolean isNamed(RoomStatus status, String expected) {
        if (status == null || status.getName() == null) return false;
        String name = java.text.Normalizer.normalize(status.getName().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase();
        return expected.equals(name);
    }

    private Booking booking(Long id) {
        return bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
    }

    private User user(Authentication authentication) {
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        return users.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private void requireVersion(Booking booking, BookingStatusActionRequest request) {
        if (request == null || request.version() == null || !request.version().equals(booking.getVersion()))
            throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");
    }

    private boolean hasLineStatus(Long id, Set<String> statusesToFind) {
        return rooms.findAllByBooking_IdBooking(id).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || pools.findAllByBooking_IdBooking(id).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || meetingRooms.findAllByBooking_IdBooking(id).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())))
                || spaces.findAllByBooking_IdBooking(id).stream().anyMatch(line -> statusesToFind.contains(code(line.getStatus())));
    }

    private String code(Booking booking) { return booking.getStatus() == null ? null : booking.getStatus().getCode(); }
    private String code(BookingLineStatus status) { return status == null ? null : status.getCode(); }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
