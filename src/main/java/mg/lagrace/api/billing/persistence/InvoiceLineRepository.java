package mg.lagrace.api.billing.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.billing.InvoiceLine;
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
 List<InvoiceLine> findAllByInvoice_IdInvoiceOrderByPositionAsc(Long invoiceId);
 boolean existsBySourceTypeAndSourceIdAndBilledTrue(String sourceType, Long sourceId);
}
