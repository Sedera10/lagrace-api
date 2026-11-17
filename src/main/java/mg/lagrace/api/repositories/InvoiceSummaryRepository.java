package mg.lagrace.api.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.views.InvoiceSummary;
public interface InvoiceSummaryRepository extends JpaRepository<InvoiceSummary, Long> {}
