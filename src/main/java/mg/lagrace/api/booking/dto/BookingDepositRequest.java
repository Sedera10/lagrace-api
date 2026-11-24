package mg.lagrace.api.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BookingDepositRequest(
        @NotNull @Positive Long methodId,
        @NotNull UUID requestId,
        @Size(max = 255) String note) {
}