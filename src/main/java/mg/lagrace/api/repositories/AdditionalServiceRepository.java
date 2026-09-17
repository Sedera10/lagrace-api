package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByName(String name);
}
