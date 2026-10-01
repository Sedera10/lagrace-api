package mg.lagrace.api.dto.room.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomTypeRateRowRequest(
    LocalDate previousValidFrom,
    LocalDate previousValidTo,
    LocalDate validFrom,
    LocalDate validTo,
    BigDecimal nightAmount,
    BigDecimal dayAmount,
    BigDecimal hourAmount
) {}
