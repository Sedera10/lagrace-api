package mg.lagrace.api.dto.room.rate;

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
