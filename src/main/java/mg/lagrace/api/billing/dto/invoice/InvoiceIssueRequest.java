package mg.lagrace.api.billing.dto.invoice;

import mg.lagrace.api.billing.Invoice;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record InvoiceIssueRequest(@NotNull Long bookingId, @NotNull List<ItemRef> items, @NotBlank String billingName, String billingAddress, String billingPhone, String billingNationality, String billingTaxId, String referenceLabel, String notes, @NotNull BigDecimal expectedTotal) {
 public record ItemRef(@NotBlank String sourceType, @NotNull Long sourceId) {}
}
