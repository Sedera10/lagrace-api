package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.MeetingRoomService;

public interface MeetingRoomServiceRepository extends JpaRepository<MeetingRoomService, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
