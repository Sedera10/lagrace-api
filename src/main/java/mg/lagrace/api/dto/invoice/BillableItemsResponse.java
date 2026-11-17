package mg.lagrace.api.dto.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record BillableItemsResponse(Customer customer, String referenceLabel, LocalDate periodStart, LocalDate periodEnd, PaymentsSummary paymentsSummary, List<Item> items, List<Item> discountItems) {
 public record Customer(String name, String phone, String nationality) {}
 public record PaymentsSummary(BigDecimal paid, List<PaymentMethod> methods) {}
 public record PaymentMethod(String method, BigDecimal amount) {}
 public record Item(String sourceType, Long sourceId, String serviceCode, String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount, boolean included) {}
}
