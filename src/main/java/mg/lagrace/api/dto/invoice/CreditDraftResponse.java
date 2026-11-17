package mg.lagrace.api.dto.invoice;

import java.math.BigDecimal;
import java.util.List;

public record CreditDraftResponse(Long invoiceId, Long bookingId, String invoiceNumber,
        String billingName, String status, BigDecimal netRemaining, BigDecimal paid, List<Line> lines) {
    public record Line(Long id, String description, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal amount, boolean discount) {}
}