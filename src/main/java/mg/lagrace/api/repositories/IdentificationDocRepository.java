package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.IdentificationDocument;

public interface IdentificationDocRepository extends JpaRepository<IdentificationDocument,Long> {
    boolean existsByName(String name);
}
