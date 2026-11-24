package mg.lagrace.api.catalog.meetingroom.persistence;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.meetingroom.MeetingRoomService;

public interface MeetingRoomServiceRepository extends JpaRepository<MeetingRoomService, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
