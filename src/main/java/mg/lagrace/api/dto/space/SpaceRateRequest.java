package mg.lagrace.api.dto.space;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpaceRateRequest(
    Long spaceId,
    Long unitId,
    Integer minPerson,
    Integer maxPerson,
    BigDecimal amount,
    LocalDate validFrom,
    LocalDate validTo
) {
    
}
