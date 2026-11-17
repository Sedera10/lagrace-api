package mg.lagrace.api.repositories;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.InvoiceLine;
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
 List<InvoiceLine> findAllByInvoice_IdInvoiceOrderByPositionAsc(Long invoiceId);
 boolean existsBySourceTypeAndSourceIdAndBilledTrue(String sourceType, Long sourceId);
}
