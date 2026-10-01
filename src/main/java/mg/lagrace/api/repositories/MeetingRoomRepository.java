package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.MeetingRoom;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    boolean existsByName(String name);

    boolean existsByIdMeetingRoom(Long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM reservation_meeting_room WHERE meeting_room_id = :id)", nativeQuery = true)
    boolean hasReservations(@Param("id") Long id);
}
