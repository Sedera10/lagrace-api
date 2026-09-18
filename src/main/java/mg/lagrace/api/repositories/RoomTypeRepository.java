package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import mg.lagrace.api.models.RoomType;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    @Override
    @EntityGraph(attributePaths = "category")
    List<RoomType> findAll();

    boolean existsByName(String name);
}
