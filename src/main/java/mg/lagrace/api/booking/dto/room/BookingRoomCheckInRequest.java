package mg.lagrace.api.booking.dto.room;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckInRequest(@NotNull Integer version, @NotNull OffsetDateTime checkInAt,
	@Size(max = 255) String reason) {
}