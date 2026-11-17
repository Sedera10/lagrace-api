package mg.lagrace.api.dto.booking.room;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckOutRequest(
        @NotNull Integer version,
        @NotNull OffsetDateTime checkOutAt,
        Boolean adjustDeparture,
        @Size(max = 255) String reason) {
}