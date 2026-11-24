package mg.lagrace.api.catalog.meetingroom.persistence;

import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.catalog.room.Room;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    List<MeetingRoom> findAllByOrderByIdMeetingRoomAsc();

    @Query("""
        SELECT room FROM MeetingRoom room
        WHERE NOT EXISTS (
            SELECT line.idBookingMeetingRoom FROM BookingMeetingRoom line
            WHERE line.meetingRoom = room AND line.serviceDate = :date AND line.active = true
              AND (:excludeId IS NULL OR line.idBookingMeetingRoom <> :excludeId)
        )
        ORDER BY room.idMeetingRoom
        """)
    List<MeetingRoom> findAvailableOn(@Param("date") LocalDate date, @Param("excludeId") Long excludeId);

    boolean existsByName(String name);

    boolean existsByIdMeetingRoom(Long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM reservation_meeting_room WHERE meeting_room_id = :id)", nativeQuery = true)
    boolean hasReservations(@Param("id") Long id);
}
