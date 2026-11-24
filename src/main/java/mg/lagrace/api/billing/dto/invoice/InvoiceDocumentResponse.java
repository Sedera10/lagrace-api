package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.employee.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import tools.jackson.databind.JsonNode;

public record InvoiceDocumentResponse(
        JsonNode issuer,
        String number,
        LocalDate issueDate,
        String documentType,
        Billing billing,
        String referenceLabel,
        Period period,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        String paymentStatus,
        String notes,
        String creditsInvoiceNumber,
        List<String> creditedByNumbers,
        boolean isCopy) {

    public record Billing(String name, String address, String phone, String taxId) {}
    public record Period(LocalDate start, LocalDate end) {}
    public record Line(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {}
}