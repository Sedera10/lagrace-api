package mg.lagrace.api.catalog.room.dto.room.rate;

import mg.lagrace.api.catalog.room.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomTypeRateUpdateRequest(
    Long unitId,
    LocalDate validFrom,
    LocalDate validTo,
    BigDecimal amount
) {}
