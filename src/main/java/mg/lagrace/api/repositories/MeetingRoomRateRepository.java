package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.MeetingRoomRate;

public interface MeetingRoomRateRepository extends JpaRepository<MeetingRoomRate, Long> {
    List<MeetingRoomRate> findAllByOrderByValidFromDesc();

    @Query(value = """
        SELECT DISTINCT ON (meeting_room_id, unit_id) *
        FROM meeting_room_rate
        WHERE service_id = CAST(:serviceId AS integer)
          AND valid_from <= :date
          AND (valid_to IS NULL OR valid_to >= :date)
        ORDER BY meeting_room_id, unit_id,
                 (valid_to IS NULL),
                 valid_from DESC,
                 id_rate DESC
        """, nativeQuery = true)
    List<MeetingRoomRate> findApplicableByService(@Param("serviceId") Long serviceId,
                                                  @Param("date") LocalDate date);

    boolean existsByMeetingRoom_IdMeetingRoom(Long meetingRoomId);
}
