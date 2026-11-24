package mg.lagrace.api.catalog.space.dto.space;

import mg.lagrace.api.catalog.space.Space;

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
