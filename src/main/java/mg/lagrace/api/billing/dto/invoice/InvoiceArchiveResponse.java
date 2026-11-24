package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceArchiveResponse(
        List<Item> items,
        Totals totals,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public record Item(
            Long id,
            Long bookingId,
            String number,
            LocalDate issueDate,
            String customerName,
            String referenceLabel,
            String serviceLabel,
            BigDecimal total,
            BigDecimal netTotal,
            String paymentMethodLabel,
            String paymentStatus,
            String status,
            String type,
            List<String> creditedByNumbers,
            String creditedInvoiceNumber) {}

    public record Totals(long count, long paidCount, BigDecimal toCollectAmount, BigDecimal totalAmount) {}
}