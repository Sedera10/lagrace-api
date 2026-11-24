package mg.lagrace.api.catalog.room.dto.room.rate;

import mg.lagrace.api.catalog.room.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomRateResponse (
        Long idRate,
        Long typeId,
        Long unitId,
        String unitName,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        boolean applicable
) {}
