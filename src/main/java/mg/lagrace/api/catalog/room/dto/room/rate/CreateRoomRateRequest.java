package mg.lagrace.api.catalog.room.dto.room.rate;

import mg.lagrace.api.catalog.room.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRateRequest(
        @NotNull Long unitId,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull LocalDate validFrom,
        LocalDate validTo            // null = permanent
) {}
