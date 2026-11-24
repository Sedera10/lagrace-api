package mg.lagrace.api.billing.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.billing.InvoiceSummary;
public interface InvoiceSummaryRepository extends JpaRepository<InvoiceSummary, Long> {}
