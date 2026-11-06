package mg.lagrace.api.dto.booking.room;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRoomExtendRequest(
        @NotNull Integer version,
        @NotNull LocalDate newCheckOut,
        String ifUnavailable,
        Long otherRoomId,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal expectedNewTotal,
        @Size(max = 255) String reason) {
}