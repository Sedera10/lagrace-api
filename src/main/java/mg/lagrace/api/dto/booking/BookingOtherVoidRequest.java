package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingOtherVoidRequest(@NotNull Integer version, @Size(max = 255) String reason) {}
