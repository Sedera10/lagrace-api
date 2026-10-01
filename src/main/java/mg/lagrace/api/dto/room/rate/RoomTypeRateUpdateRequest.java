package mg.lagrace.api.dto.room.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomTypeRateUpdateRequest(
    Long unitId,
    LocalDate validFrom,
    LocalDate validTo,
    BigDecimal amount
) {}
