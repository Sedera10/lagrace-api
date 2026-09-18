package mg.lagrace.api.dto.room;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomRateResponse(
    Long idRate,
    BigDecimal amount,
    Integer typeId,
    LocalDate validFrom,
    LocalDate validTo
) {
    
}
