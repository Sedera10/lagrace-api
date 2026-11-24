package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
public record InvoiceDetailResponse(Long id, String invoiceNumber, String invoiceType, String status, LocalDate issueDate, Long bookingId, Long customerId,
 String billingName, String billingAddress, String billingPhone, String billingNationality, String billingTaxId, String referenceLabel,
 LocalDate periodStart, LocalDate periodEnd, BigDecimal subtotal, BigDecimal discount, BigDecimal total, String notes, String issuerSnapshot,
 String paymentStatus, BigDecimal paid, BigDecimal balance, String creditedInvoiceNumber, List<Credit> creditedBy, List<Line> lines, List<String> paymentMethods, OffsetDateTime createdAt) {
 public InvoiceDetailResponse(Long id, String invoiceNumber, String invoiceType, String status, LocalDate issueDate, Long bookingId, Long customerId,
  String billingName, String billingAddress, String billingPhone, String billingNationality, String billingTaxId, String referenceLabel,
  LocalDate periodStart, LocalDate periodEnd, BigDecimal subtotal, BigDecimal discount, BigDecimal total, String notes, String issuerSnapshot,
  String paymentStatus, BigDecimal paid, BigDecimal balance, String creditedInvoiceNumber, List<Credit> creditedBy, List<Line> lines, OffsetDateTime createdAt) {
  this(id, invoiceNumber, invoiceType, status, issueDate, bookingId, customerId, billingName, billingAddress, billingPhone, billingNationality, billingTaxId, referenceLabel, periodStart, periodEnd, subtotal, discount, total, notes, issuerSnapshot, paymentStatus, paid, balance, creditedInvoiceNumber, creditedBy, lines, List.of(), createdAt);
 }
 public record Line(Long id, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, Integer position, String serviceCode, String sourceType, Long sourceId, Boolean billed, Long creditedLineId) {}
 public record Credit(Long id, String invoiceNumber, BigDecimal total, LocalDate issueDate) {}
}
