package mg.lagrace.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookingOtherCreateRequest(@NotBlank @Size(max = 150) String label,
        @NotNull @Positive BigDecimal amount) {}
