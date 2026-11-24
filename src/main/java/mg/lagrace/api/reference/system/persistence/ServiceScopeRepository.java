package mg.lagrace.api.reference.system.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.ServiceScope;

public interface ServiceScopeRepository extends JpaRepository<ServiceScope, Long> {
    boolean existsByCode(String code);
}
