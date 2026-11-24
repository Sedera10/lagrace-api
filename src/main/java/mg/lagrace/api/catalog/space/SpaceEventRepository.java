package mg.lagrace.api.catalog.space;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceEventRepository extends JpaRepository<SpaceEvent, Long> {
    boolean existsByName(String name);
}
