package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Room;

public interface RoomRepository extends JpaRepository<Room,Long> {
    
}
