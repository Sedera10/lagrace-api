package mg.lagrace.api.catalog.space.dto.space;

import mg.lagrace.api.catalog.space.Space;

public record SpaceRequest(String name, String description, Integer capacity) {}
