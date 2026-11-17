package mg.lagrace.api.dto.booking.pool;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.NewCustomer;
import mg.lagrace.api.models.enums.BookingOrigin;

public record BookingPoolRequest(
        Long customerId,
        @Valid NewCustomer newCustomer,
        BookingOrigin origin,
        String notes,
        @NotNull @Valid PoolItem pool,
        List<@Valid ExtraItem> extras,
        Boolean confirm,
        @DecimalMin(value = "0.00") BigDecimal expectedTotal) {

    public record PoolItem(
            @NotNull Long poolId,
            @NotNull Long poolServiceId,
            @NotNull OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            @Min(1) Integer guestsCount,
            @NotNull @Valid List<@NotNull RateItem> lines) {}

        public record RateItem(@NotNull Long poolRateId, @NotNull @Min(1) Integer quantity) {}
}