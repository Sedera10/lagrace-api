package mg.lagrace.api.catalog.restaurant.dto.menu;

public record MenuItemRequest(
        Long categoryId,
        String name,
        String code,
        String description,
        Boolean active) {
}