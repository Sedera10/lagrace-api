package mg.lagrace.api.catalog.additionalservice.dto.service;

import mg.lagrace.api.catalog.additionalservice.AdditionalService;

public record ServiceRequest(String code, String name, String description, Boolean isExculusive) {}
