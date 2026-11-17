package mg.lagrace.api.dto.booking;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookingPaymentRequest(
        @NotNull Integer version,
        @NotNull @Positive Long methodId,
        @NotNull @Pattern(regexp = "PAYMENT|REFUND") String kind,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Size(max = 255) String note) {
}