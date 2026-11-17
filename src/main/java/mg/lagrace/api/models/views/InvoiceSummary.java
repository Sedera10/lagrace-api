package mg.lagrace.api.models.views;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;
import jakarta.persistence.*;

@Entity @Immutable @Table(name="v_invoice_summary")
public class InvoiceSummary {
 @Id @Column(name="id_invoice") private Long idInvoice; @Column(name="invoice_number") private String invoiceNumber; @Column(name="invoice_type") private String invoiceType; @Column(name="booking_id") private Long bookingId; @Column(name="customer_id") private Long customerId; @Column(name="status_code") private String statusCode; @Column(name="issue_date") private LocalDate issueDate; @Column(name="net_total") private BigDecimal netTotal; @Column(name="booking_paid") private BigDecimal bookingPaid; @Column(name="paid_on_invoice") private BigDecimal paidOnInvoice; @Column(name="payment_status") private String paymentStatus;
 public Long getIdInvoice(){return idInvoice;} public String getInvoiceNumber(){return invoiceNumber;} public String getInvoiceType(){return invoiceType;} public Long getBookingId(){return bookingId;} public Long getCustomerId(){return customerId;} public String getStatusCode(){return statusCode;} public LocalDate getIssueDate(){return issueDate;} public BigDecimal getNetTotal(){return netTotal;} public BigDecimal getBookingPaid(){return bookingPaid;} public BigDecimal getPaidOnInvoice(){return paidOnInvoice;} public String getPaymentStatus(){return paymentStatus;}
}
