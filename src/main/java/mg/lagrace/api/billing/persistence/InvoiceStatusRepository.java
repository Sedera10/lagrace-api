package mg.lagrace.api.billing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.billing.InvoiceStatus;

import java.util.Optional;

public interface InvoiceStatusRepository extends JpaRepository<InvoiceStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    Optional<InvoiceStatus> findByCode(String code);
}
