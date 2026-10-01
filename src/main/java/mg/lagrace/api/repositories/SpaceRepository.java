package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Space;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    boolean existsByName(String name);
}