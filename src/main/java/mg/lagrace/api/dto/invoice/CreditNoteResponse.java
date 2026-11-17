package mg.lagrace.api.dto.invoice;

import java.math.BigDecimal;

public record CreditNoteResponse(InvoiceDetailResponse invoice, BigDecimal refundSuggested) {}