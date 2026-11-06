package mg.lagrace.api.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingCancelRequest(
        @NotNull Integer version,
        @NotBlank @Size(max = 255) String reason,
        @Valid BookingCancellationFee cancellationFee) {
}