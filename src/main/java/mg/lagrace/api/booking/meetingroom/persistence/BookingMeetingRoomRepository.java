package mg.lagrace.api.booking.meetingroom.persistence;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.meetingroom.MeetingRoom;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;

public interface BookingMeetingRoomRepository extends JpaRepository<BookingMeetingRoom, Long> {
    boolean existsByRef(String ref);
    List<BookingMeetingRoom> findAllByBooking_IdBooking(Long bookingId);

    @Query("SELECT COUNT(line) > 0 FROM BookingMeetingRoom line WHERE line.meetingRoom.idMeetingRoom = :roomId AND line.serviceDate = :serviceDate AND line.active = true")
    boolean existsActiveBookingForRoomAndDate(@Param("roomId") Long roomId, @Param("serviceDate") java.time.LocalDate serviceDate);

    @Query("SELECT COUNT(line) > 0 FROM BookingMeetingRoom line WHERE line.meetingRoom.idMeetingRoom = :roomId AND line.serviceDate = :serviceDate AND line.active = true AND line.idBookingMeetingRoom <> :excludeId")
    boolean existsActiveBookingForRoomAndDateExcluding(@Param("roomId") Long roomId, @Param("serviceDate") java.time.LocalDate serviceDate, @Param("excludeId") Long excludeId);
}
