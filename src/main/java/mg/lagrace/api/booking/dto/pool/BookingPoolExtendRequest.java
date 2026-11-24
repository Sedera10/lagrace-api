package mg.lagrace.api.booking.dto.pool;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.pool.Pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;

public record BookingPoolExtendRequest(@NotNull Integer version, @NotNull OffsetDateTime newEndsAt,
        BigDecimal expectedNewTotal, String reason) {}
