package mg.lagrace.api.booking.dto.room;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckOutRequest(
        @NotNull Integer version,
        @NotNull OffsetDateTime checkOutAt,
        Boolean adjustDeparture,
        @Size(max = 255) String reason) {
}