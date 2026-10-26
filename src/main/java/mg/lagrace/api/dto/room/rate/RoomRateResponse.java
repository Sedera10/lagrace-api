package mg.lagrace.api.dto.room.rate;

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
