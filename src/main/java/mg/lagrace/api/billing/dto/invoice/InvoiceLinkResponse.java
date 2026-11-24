package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceLinkResponse(Long id, String number, LocalDate issueDate, BigDecimal total,
        String type, String status) {}