package mg.lagrace.api.dto.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PoolRateRequest(
    Long poolId,
    Long serviceId,
    Long unitId,
    Boolean mealIncluded,
    Integer minAge,
    Integer maxAge,
    BigDecimal amount,
    LocalDate validFrom,
    LocalDate validTo
) {}
