package mg.lagrace.api.catalog.additionalservice.dto.service;

import mg.lagrace.api.catalog.additionalservice.AdditionalService;

import java.math.BigDecimal;

import mg.lagrace.api.catalog.additionalservice.CateringCondition;

public record AdditionalServiceRequest(
        String name,
        Long scopeId,
        Long unitId,
        BigDecimal price,
        Boolean deposit,
        CateringCondition cateringCondition,
        Boolean freeWithCaterer,
        Boolean active) {}
