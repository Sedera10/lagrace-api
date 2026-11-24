package mg.lagrace.api.catalog.space;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    boolean existsByName(String name);
}