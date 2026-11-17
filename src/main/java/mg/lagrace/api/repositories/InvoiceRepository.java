package mg.lagrace.api.repositories;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import mg.lagrace.api.models.Invoice;
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
 List<Invoice> findAllByCreditedInvoice_IdInvoice(Long invoiceId);
 List<Invoice> findAllByBooking_IdBookingOrderByIssueDateDescIdInvoiceDesc(Long bookingId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select i from Invoice i where i.idInvoice = :id")
 Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
}
