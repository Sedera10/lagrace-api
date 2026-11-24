package mg.lagrace.api.catalog.room.dto.room;

import mg.lagrace.api.catalog.room.Room;

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
