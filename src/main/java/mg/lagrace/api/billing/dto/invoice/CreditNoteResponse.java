package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;

import java.math.BigDecimal;

public record CreditNoteResponse(InvoiceDetailResponse invoice, BigDecimal refundSuggested) {}