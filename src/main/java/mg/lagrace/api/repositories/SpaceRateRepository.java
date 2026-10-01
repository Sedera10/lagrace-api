package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.SpaceRate;

public interface SpaceRateRepository extends JpaRepository<SpaceRate, Long> {
    List<SpaceRate> findAllBySpace_IdSpace(Long idSpace);
    List<SpaceRate> findAllBySpace(Space space);
}
