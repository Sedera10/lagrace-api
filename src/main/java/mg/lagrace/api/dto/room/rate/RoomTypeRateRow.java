package mg.lagrace.api.dto.room.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomTypeRateRow(
    LocalDate validFrom,
    LocalDate validTo,
    BigDecimal nightAmount,
    BigDecimal dayAmount,
    BigDecimal hourAmount
) {}
