package mg.lagrace.api.dto.booking.room;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BookingRoomUpdateRequest(
        @NotNull Integer version,
        Long roomId,
        LocalDate checkIn,
        LocalDate checkOut,
        String reason,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal expectedNewTotal) {
}
