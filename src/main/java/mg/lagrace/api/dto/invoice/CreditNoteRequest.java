package mg.lagrace.api.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

public record CreditNoteRequest(@NotEmpty List<@NotNull Long> lineIds,
	@NotBlank @Size(max = 255) String reason,
	@NotNull @DecimalMin("0.00") BigDecimal expectedTotal) {}