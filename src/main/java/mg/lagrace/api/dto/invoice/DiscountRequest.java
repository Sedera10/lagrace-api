package mg.lagrace.api.dto.invoice;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record DiscountRequest(@NotBlank String label, @NotNull @DecimalMin(value="0.01") BigDecimal amount) {}
