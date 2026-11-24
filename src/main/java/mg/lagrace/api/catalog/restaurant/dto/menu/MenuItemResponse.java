package mg.lagrace.api.catalog.restaurant.dto.menu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MenuItemResponse(
        Long idMenuItem,
        Long categoryId,
        String categoryName,
        Integer categoryPosition,
        String name,
        String code,
        String description,
        Boolean active,
        OffsetDateTime createdAt,
        Long rateId,
        BigDecimal unitPrice,
        LocalDate rateValidFrom,
        LocalDate rateValidTo) {
}