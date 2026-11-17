package mg.lagrace.api.dto.menu;

public record MenuItemRequest(
        Long categoryId,
        String name,
        String code,
        String description,
        Boolean active) {
}