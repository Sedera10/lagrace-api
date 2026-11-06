package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.NotNull;

public record BookingStatusActionRequest(@NotNull Integer version) {
}