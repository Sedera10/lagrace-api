package mg.lagrace.api.dto.booking.room;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomCheckOutRequest(
        @NotNull Integer version,
        Boolean adjustDeparture,
        @Size(max = 255) String reason) {
}