package mg.lagrace.api.booking.application;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;

import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.booking.pool.persistence.BookingPoolRepository;
import mg.lagrace.api.booking.room.persistence.BookingRoomRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingLineStatusRepository;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.core.BookingStatusHistory;
import mg.lagrace.api.booking.core.BookingStatusHistoryRepository;
import mg.lagrace.api.booking.core.BookingStatusRepository;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.booking.pool.BookingPool;
import mg.lagrace.api.booking.room.BookingRoom;
import mg.lagrace.api.booking.space.BookingSpace;
import mg.lagrace.api.booking.dto.BookingStatusRequest;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;


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
    private final BookingChangeLogger changeLogger;

    public BookingStatusService(BookingStatusRepository bookingStatuses, BookingLineStatusRepository lineStatuses,
            BookingStatusHistoryRepository history, BookingRepository bookings, BookingRoomRepository rooms,
            BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms,
            BookingSpaceRepository spaces, UserRepository users, BookingChangeLogger changeLogger) {
        this.bookingStatuses = bookingStatuses; this.lineStatuses = lineStatuses; this.history = history;
        this.bookings = bookings; this.rooms = rooms; this.pools = pools; this.meetingRooms = meetingRooms;
        this.spaces = spaces; this.users = users; this.changeLogger = changeLogger;
    }

    @Transactional
    public void changeLineStatus(Object line, String newCode, String reason, Long userId) {
        changeLineStatus(line, newCode, reason, userId, null);
    }

    @Transactional
    public void changeLineStatus(Object line, String newCode, String reason, Long userId,
            OffsetDateTime changedAt) {
        LineMutation mutation = lineMutation(line, newCode);
        change(mutation, newCode, reason, userId, changedAt);
    }

    @Transactional
    public Booking changeBookingStatus(Booking booking, String newCode, String reason, Long userId) {
        return changeBookingStatus(booking, newCode, reason, userId, null);
        }

        @Transactional
        public Booking changeBookingStatus(Booking booking, String newCode, String reason, Long userId,
            OffsetDateTime changedAt) {
        String old = code(booking.getStatus());
        if (!isBookingTransitionAllowed(old, newCode)) throw forbidden("Transition de dossier interdite : " + old + " → " + newCode + ".");
        BookingStatus status = requireBookingStatus(newCode);
        booking.setStatus(status);
        bookings.save(booking);
        saveHistory(booking, null, null, old, newCode, reason, userId, changedAt);
        return booking;
    }

    public void assertBookingOpen(Booking booking) {
        String code = code(booking.getStatus());
        if (Set.of("CLOSED", "CANCELED").contains(code)) throw forbidden("Ce dossier est fermé et ne peut plus être modifié.");
    }

    private void change(LineMutation mutation, String next, String reason, Long userId, OffsetDateTime changedAt) {
        String old = mutation.statusCode();
        if (!Map.of("CONFIRMED", Set.of("CHECK_IN", "CANCELED"), "CHECK_IN", Set.of("CHECK_OUT"))
                .getOrDefault(old, Set.of()).contains(next)) {
            throw forbidden("Transition de ligne interdite : " + old + " → " + next + ".");
        }
        BookingLineStatus status = lineStatuses.findByCode(next);
        if (status == null) throw forbidden("Statut de ligne inconnu : " + next + ".");
        mutation.update().accept(status);
        saveHistory(mutation.booking(), mutation.type(), mutation.id(), old, next, reason, userId, changedAt);
        changeLogger.log(mutation.booking().getIdBooking(), mutation.type(), mutation.id(), "UPDATE",
                "status", old, next, reason, userId, UUID.randomUUID());
    }

    private LineMutation lineMutation(Object line, String nextStatus) {
        if (line instanceof BookingRoom room)
            return new LineMutation("ROOM", room.getIdBookingRoom(), room.getBooking(), code(room.getStatus()),
                    status -> { room.setStatus(status); room.setActive(reservesAvailability(nextStatus)); rooms.save(room); });
        if (line instanceof BookingPool pool)
            return new LineMutation("POOL", pool.getIdBookingPool(), pool.getBooking(), code(pool.getStatus()),
                    status -> { pool.setStatus(status); pool.setActive(reservesAvailability(nextStatus)); pools.save(pool); });
        if (line instanceof BookingMeetingRoom meetingRoom)
            return new LineMutation("MEETING_ROOM", meetingRoom.getIdBookingMeetingRoom(), meetingRoom.getBooking(), code(meetingRoom.getStatus()),
                    status -> { meetingRoom.setStatus(status); meetingRoom.setActive(reservesAvailability(nextStatus)); meetingRooms.save(meetingRoom); });
        if (line instanceof BookingSpace space)
            return new LineMutation("SPACE", space.getIdBookingSpace(), space.getBooking(), code(space.getStatus()),
                    status -> { space.setStatus(status); space.setActive(reservesAvailability(nextStatus)); spaces.save(space); });
        throw new IllegalArgumentException("Type de ligne non pris en charge : "
                + (line == null ? "null" : line.getClass().getName()));
    }

    private record LineMutation(String type, Long id, Booking booking, String statusCode,
            Consumer<BookingLineStatus> update) {}

    private boolean isBookingTransitionAllowed(String old, String next) {
        if (Set.of("CLOSED", "CANCELED").contains(old)) return false;
        return "CANCELED".equals(next) || Map.of("PENDING", Set.of("CONFIRMED", "IN_PROGRESS"),
                "CONFIRMED", Set.of("IN_PROGRESS"), "IN_PROGRESS", Set.of("CLOSED"))
                .getOrDefault(old, Set.of()).contains(next);
    }

    private String code(BookingStatus status) { return status == null ? null : status.getCode(); }
    private String code(BookingLineStatus status) { return status == null ? null : status.getCode(); }
    private boolean reservesAvailability(String statusCode) {
        return !Set.of("CANCELED", "CHECK_OUT").contains(statusCode);
    }
    private BookingStatus requireBookingStatus(String code) {
        BookingStatus result = bookingStatuses.findByCode(code);
        if (result == null) throw forbidden("Statut de dossier inconnu : " + code + ".");
        return result;
    }
    private BusinessException forbidden(String message) { return new BusinessException("FORBIDDEN_TRANSITION", message); }
    private void saveHistory(Booking booking, String type, Long id, String old, String next, String reason, Long userId,
            OffsetDateTime changedAt) {
        BookingStatusHistory item = new BookingStatusHistory();
        item.setBooking(booking); item.setLineType(type); item.setLineId(id == null ? null : id.toString());
        item.setOldStatusCode(old); item.setNewStatusCode(next); item.setReason(reason);
        item.setChangedAt(changedAt);
        if (userId != null) users.findById(userId).ifPresent(item::setChangedByUser);
        history.save(item);
    }
    public List<BookingStatus> getAllBokingStatus(){
        return bookingStatuses.findAll();
    }
    public List<BookingLineStatus> getAllBokingLineStatus(){
        return lineStatuses.findAll();
    }

    public BookingStatus addBookingStatus(BookingStatusRequest request) {
        String code = code(request == null ? null : request.code(), "du statut de dossier");
        String name = name(request == null ? null : request.name(), "du statut de dossier");
        if (bookingStatuses.existsByCode(code) || bookingStatuses.existsByName(name)) {
            throw new DuplicateResourceException("Le statut de dossier existe déjà.");
        }
        BookingStatus status = new BookingStatus();
        status.setCode(code);
        status.setName(name);
        status.setColorCode(request.colorCode());
        return bookingStatuses.save(status);
    }

    public BookingStatus updateBookingStatus(Long id, BookingStatusRequest request) {
        BookingStatus status = bookingStatuses.findById(id)
                .orElseThrow(() -> notFound("Statut de dossier", id));
        String code = code(request == null ? null : request.code(), "du statut de dossier");
        String name = name(request == null ? null : request.name(), "du statut de dossier");
        if ((!status.getCode().equals(code) && bookingStatuses.existsByCode(code))
                || (!status.getName().equals(name) && bookingStatuses.existsByName(name))) {
            throw new DuplicateResourceException("Le statut de dossier existe déjà.");
        }
        status.setCode(code);
        status.setName(name);
        status.setColorCode(request.colorCode());
        return bookingStatuses.save(status);
    }

    public void deleteBookingStatus(Long id) {
        bookingStatuses.delete(find(bookingStatuses, id, "Statut de dossier"));
    }

    public BookingLineStatus addBookingLineStatus(BookingStatusRequest request) {
        String code = code(request == null ? null : request.code(), "du statut de ligne");
        String name = name(request == null ? null : request.name(), "du statut de ligne");
        if (lineStatuses.existsByCode(code) || lineStatuses.existsByName(name)) {
            throw new DuplicateResourceException("Le statut de ligne existe déjà.");
        }
        BookingLineStatus status = new BookingLineStatus();
        status.setCode(code);
        status.setName(name);
        status.setColorCode(request.colorCode());
        return lineStatuses.save(status);
    }

    public BookingLineStatus updateBookingLineStatus(Long id, BookingStatusRequest request) {
        BookingLineStatus status = lineStatuses.findById(id)
                .orElseThrow(() -> notFound("Statut de ligne", id));
        String code = code(request == null ? null : request.code(), "du statut de ligne");
        String name = name(request == null ? null : request.name(), "du statut de ligne");
        if ((!status.getCode().equals(code) && lineStatuses.existsByCode(code))
                || (!status.getName().equals(name) && lineStatuses.existsByName(name))) {
            throw new DuplicateResourceException("Le statut de ligne existe déjà.");
        }
        status.setCode(code);
        status.setName(name);
        status.setColorCode(request.colorCode());
        return lineStatuses.save(status);
    }

    public void deleteBookingLineStatus(Long id) {
        lineStatuses.delete(find(lineStatuses, id, "Statut de ligne"));
    }

    private String name(String value, String label) {
        if (value == null || value.isBlank()) throw new ValidationException("Le nom " + label + " est obligatoire.");
        return value.trim();
    }

    private String code(String value, String label) {
        if (value == null || value.isBlank()) throw new ValidationException("Le code " + label + " est obligatoire.");
        return value.trim().toUpperCase();
    }

    private <T> T find(org.springframework.data.jpa.repository.JpaRepository<T, Long> repository,
            Long id, String label) {
        if (id == null) throw new ValidationException("L'identifiant de " + label.toLowerCase() + " est obligatoire.");
        return repository.findById(id).orElseThrow(() -> notFound(label, id));
    }

    private ResourceNotFoundException notFound(String label, Long id) {
        return new ResourceNotFoundException(label + " introuvable avec l'id " + id);
    }
}
