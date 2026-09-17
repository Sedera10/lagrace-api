package mg.lagrace.api.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.RoomStatus;

public interface RoomStatusRepository extends JpaRepository<RoomStatus, Long>{
    Optional<RoomStatus> findByName(String name);
    boolean existsByName(String name);
}
