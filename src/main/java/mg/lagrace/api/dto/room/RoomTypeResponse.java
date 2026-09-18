package mg.lagrace.api.dto.room;

import java.time.OffsetDateTime;

public record RoomTypeResponse(
        Long idType,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        Boolean isAirConditioned,
        Integer defaultCapacity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
