package mg.lagrace.api.catalog.room.persistence;

import mg.lagrace.api.catalog.room.Room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.room.RoomStatus;

public interface RoomStatusRepository extends JpaRepository<RoomStatus, Long>{
    Optional<RoomStatus> findByName(String name);
    boolean existsByName(String name);
    List<RoomStatus> findAllByOrderByIdStatus();
}
