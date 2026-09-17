package mg.lagrace.api.dto.service;

import java.math.BigDecimal;

public record AdditionalServiceRequest(
        String name,
        Long scopeId,
        Long unitId,
        BigDecimal price,
        Boolean deposit,
        Boolean active) {}
