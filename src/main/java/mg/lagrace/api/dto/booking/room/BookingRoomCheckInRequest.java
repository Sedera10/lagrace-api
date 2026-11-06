package mg.lagrace.api.dto.booking.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckInRequest(@NotNull Integer version, @Size(max = 255) String reason) {
}