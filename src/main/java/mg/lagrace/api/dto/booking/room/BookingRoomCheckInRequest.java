package mg.lagrace.api.dto.booking.room;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckInRequest(@NotNull Integer version, @NotNull OffsetDateTime checkInAt,
	@Size(max = 255) String reason) {
}