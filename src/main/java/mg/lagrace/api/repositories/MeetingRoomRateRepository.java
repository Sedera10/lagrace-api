package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.MeetingRoomRate;

public interface MeetingRoomRateRepository extends JpaRepository<MeetingRoomRate, Long> {
    List<MeetingRoomRate> findAllByOrderByValidFromDesc();

    boolean existsByMeetingRoom_IdMeetingRoom(Long meetingRoomId);
}
