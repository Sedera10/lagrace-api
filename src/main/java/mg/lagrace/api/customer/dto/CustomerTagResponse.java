package mg.lagrace.api.customer.dto;

public record CustomerTagResponse(
    Long idTag, 
    String name,
    String colorCode, 
    boolean isActive
) {}