package mg.lagrace.api.booking.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookingCancellationFee(
        @NotBlank @Size(max = 150) String label,
        @NotNull @Positive BigDecimal amount) {
}