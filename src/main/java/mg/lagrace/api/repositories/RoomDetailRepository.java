package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.views.RoomDetail;

public interface RoomDetailRepository extends JpaRepository<RoomDetail,Long> {
    
}