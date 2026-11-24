package mg.lagrace.api.booking.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BookingOtherRequest(
        Long bookingId,
        String label,
        BigDecimal amount,
        OffsetDateTime voidedAt,
        String voidReason) {
}
