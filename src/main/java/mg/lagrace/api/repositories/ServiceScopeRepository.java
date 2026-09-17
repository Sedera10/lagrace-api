package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.ServiceScope;

public interface ServiceScopeRepository extends JpaRepository<ServiceScope, Long> {
    boolean existsByCode(String code);
}
