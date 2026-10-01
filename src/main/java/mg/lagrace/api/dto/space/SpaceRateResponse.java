package mg.lagrace.api.dto.space;

import java.math.BigDecimal;
import java.time.LocalDate;
import mg.lagrace.api.models.SpaceRate;

public record SpaceRateResponse(
    Long idRate,
    Long spaceId,
    Long unitId,
    String unitName,
    Integer minPerson,
    Integer maxPerson,
    BigDecimal amount,
    LocalDate validFrom,
    LocalDate validTo
) {
    public static SpaceRateResponse from(SpaceRate item) {
        return new SpaceRateResponse(
                item.getIdRate(),
                item.getSpace() != null ? item.getSpace().getIdSpace() : null,
                item.getUnit() != null ? item.getUnit().getIdUnit() : null,
                item.getUnit() != null ? item.getUnit().getName() : null,
                item.getMinPerson(),
                item.getMaxPerson(),
                item.getAmount(),
                item.getValidFrom(),
                item.getValidTo()
        );
    }
}
