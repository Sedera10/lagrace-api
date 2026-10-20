package mg.lagrace.api.dto.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import mg.lagrace.api.models.PoolRate;

public record PoolRateResponse(
        Long idRate,
        Long poolId,
        Long serviceId,
        String serviceName,
        Long unitId,
        String unitName,
        Boolean isMealIncluded,
        Integer minAge,
        Integer maxAge,
        String ageLabel,
        String mealLabel,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        OffsetDateTime createdAt
) {
    public static PoolRateResponse from(PoolRate rate) {
        return new PoolRateResponse(
                rate.getIdRate(),
                rate.getPool() != null ? rate.getPool().getIdPool() : null,
                rate.getService() != null ? rate.getService().getIdService() : null,
                rate.getService() != null ? rate.getService().getName() : null,
                rate.getUnit() != null ? rate.getUnit().getIdUnit() : null,
                rate.getUnit() != null ? rate.getUnit().getName() : null,
                rate.getIsMealIncluded(),
                rate.getMinAge(),
                rate.getMaxAge(),
                rate.getAgeLabel(),
                rate.getMealLabel(),
                rate.getAmount(),
                rate.getValidFrom(),
                rate.getValidTo(),
                rate.getCreatedAt()
        );
    }
}
