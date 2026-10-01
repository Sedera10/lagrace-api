package mg.lagrace.api.dto.service;

import java.math.BigDecimal;
import mg.lagrace.api.models.enums.CateringCondition;

public record AdditionalServiceRequest(
        String name,
        Long scopeId,
        Long unitId,
        BigDecimal price,
        Boolean deposit,
        CateringCondition cateringCondition,
        Boolean active) {}
