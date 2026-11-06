package mg.lagrace.api.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BookingOtherRequest(
        Long bookingId,
        String label,
        BigDecimal amount,
        OffsetDateTime voidedAt,
        String voidReason) {
}
