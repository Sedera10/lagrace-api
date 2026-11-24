package mg.lagrace.api.booking.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.UUID;

public record BookingDepositReturnRequest(
        @NotNull @Positive Long methodId,
        @NotNull UUID requestId,
        @Valid Withholding withholding,
        @Size(max = 255) String note) {

    public record Withholding(@NotNull @Size(max = 150) String label,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}
}