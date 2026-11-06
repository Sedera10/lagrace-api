package mg.lagrace.api.services;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.models.*;
import mg.lagrace.api.repositories.*;

@Service
public class BookingStatusService {
    private final BookingStatusRepository bookingStatuses;
    private final BookingLineStatusRepository lineStatuses;
    private final BookingStatusHistoryRepository history;
    private final BookingRepository bookings;
    private final BookingRoomRepository rooms;
    private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms;
    private final BookingSpaceRepository spaces;
    private final UserRepository users;

    public BookingStatusService(BookingStatusRepository bookingStatuses, BookingLineStatusRepository lineStatuses,
            BookingStatusHistoryRepository history, BookingRepository bookings, BookingRoomRepository rooms,
            BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms,
            BookingSpaceRepository spaces, UserRepository users) {
        this.bookingStatuses = bookingStatuses; this.lineStatuses = lineStatuses; this.history = history;
        this.bookings = bookings; this.rooms = rooms; this.pools = pools; this.meetingRooms = meetingRooms;
        this.spaces = spaces; this.users = users;
    }

    @Transactional
    public BookingRoom changeLineStatus(BookingRoom line, String newCode, String reason, Long userId) {
        return change(line, newCode, reason, userId, "ROOM", line.getIdBookingRoom(), line.getBooking(),
                line.getStatus() == null ? null : line.getStatus().getCode(),
                status -> { line.setStatus(status); line.setActive(!"CANCELED".equals(newCode)); rooms.save(line); });
    }

    @Transactional
    public BookingPool changeLineStatus(BookingPool line, String newCode, String reason, Long userId) {
        return change(line, newCode, reason, userId, "POOL", line.getIdBookingPool(), line.getBooking(),
                line.getStatus() == null ? null : line.getStatus().getCode(),
                status -> { line.setStatus(status); line.setActive(!"CANCELED".equals(newCode)); pools.save(line); });
    }

    @Transactional
    public BookingMeetingRoom changeLineStatus(BookingMeetingRoom line, String newCode, String reason, Long userId) {
        return change(line, newCode, reason, userId, "MEETING_ROOM", line.getIdBookingMeetingRoom(), line.getBooking(),
                line.getStatus() == null ? null : line.getStatus().getCode(),
                status -> { line.setStatus(status); line.setActive(!"CANCELED".equals(newCode)); meetingRooms.save(line); });
    }

    @Transactional
    public BookingSpace changeLineStatus(BookingSpace line, String newCode, String reason, Long userId) {
        return change(line, newCode, reason, userId, "SPACE", line.getIdBookingSpace(), line.getBooking(),
                line.getStatus() == null ? null : line.getStatus().getCode(),
                status -> { line.setStatus(status); line.setActive(!"CANCELED".equals(newCode)); spaces.save(line); });
    }

    @Transactional
    public Booking changeBookingStatus(Booking booking, String newCode, String reason, Long userId) {
        String old = code(booking.getStatus());
        if (!isBookingTransitionAllowed(old, newCode)) throw forbidden("Transition de dossier interdite : " + old + " → " + newCode + ".");
        BookingStatus status = requireBookingStatus(newCode);
        booking.setStatus(status);
        bookings.save(booking);
        saveHistory(booking, null, null, old, newCode, reason, userId);
        return booking;
    }

    public void assertBookingOpen(Booking booking) {
        String code = code(booking.getStatus());
        if (Set.of("CLOSED", "CANCELED").contains(code)) throw forbidden("Ce dossier est fermé et ne peut plus être modifié.");
    }

    private <T> T change(T line, String next, String reason, Long userId, String type, Long id, Booking booking,
            String old, java.util.function.Consumer<BookingLineStatus> updater) {
        if (!Map.of("CONFIRMED", Set.of("CHECK_IN", "CANCELED"), "CHECK_IN", Set.of("CHECK_OUT"))
                .getOrDefault(old, Set.of()).contains(next)) {
            throw forbidden("Transition de ligne interdite : " + old + " → " + next + ".");
        }
        BookingLineStatus status = lineStatuses.findByCode(next);
        if (status == null) throw forbidden("Statut de ligne inconnu : " + next + ".");
        updater.accept(status);
        saveHistory(booking, type, id, old, next, reason, userId);
        return line;
    }

    private boolean isBookingTransitionAllowed(String old, String next) {
        if (Set.of("CLOSED", "CANCELED").contains(old)) return false;
        return "CANCELED".equals(next) || Map.of("PENDING", Set.of("CONFIRMED", "IN_PROGRESS"),
                "CONFIRMED", Set.of("IN_PROGRESS"), "IN_PROGRESS", Set.of("CLOSED"))
                .getOrDefault(old, Set.of()).contains(next);
    }

    private String code(BookingStatus status) { return status == null ? null : status.getCode(); }
    private BookingStatus requireBookingStatus(String code) {
        BookingStatus result = bookingStatuses.findByCode(code);
        if (result == null) throw forbidden("Statut de dossier inconnu : " + code + ".");
        return result;
    }
    private BusinessException forbidden(String message) { return new BusinessException("FORBIDDEN_TRANSITION", message); }
    private void saveHistory(Booking booking, String type, Long id, String old, String next, String reason, Long userId) {
        BookingStatusHistory item = new BookingStatusHistory();
        item.setBooking(booking); item.setLineType(type); item.setLineId(id == null ? null : id.toString());
        item.setOldStatusCode(old); item.setNewStatusCode(next); item.setReason(reason);
        if (userId != null) users.findById(userId).ifPresent(item::setChangedByUser);
        history.save(item);
    }
}
