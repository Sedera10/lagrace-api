package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import mg.lagrace.api.models.InvoiceStatus;

public interface InvoiceStatusRepository extends JpaRepository<InvoiceStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    Optional<InvoiceStatus> findByCode(String code);
}
