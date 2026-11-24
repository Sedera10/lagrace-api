package mg.lagrace.api.booking.dto.pool;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.pool.Pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Fields are deliberately nullable: PATCH keeps an omitted value unchanged. */
public record BookingPoolUpdateRequest(Integer version, Long poolId, Long poolServiceId,
        OffsetDateTime startsAt, OffsetDateTime endsAt, Integer guestsCount, Long poolRateId,
        List<RateItem> lines, String reason, BigDecimal expectedNewTotal) {
    public record RateItem(Long poolRateId, Integer quantity) {}
}
