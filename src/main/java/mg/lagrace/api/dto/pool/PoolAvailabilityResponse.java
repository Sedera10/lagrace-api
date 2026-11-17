package mg.lagrace.api.dto.pool;

import java.time.OffsetDateTime;

public record PoolAvailabilityResponse(Long lineId, String bookingRef, Long poolId, String poolLabel,
        String formulaLabel, boolean exclusive, OffsetDateTime startsAt, OffsetDateTime endsAt,
        Integer guestsCount, Integer quantity) {}