package mg.lagrace.api.catalog.room.dto.room;

import mg.lagrace.api.catalog.room.Room;

public record RoomTypeRequest(
    String name,
    String description,
    Boolean isAirConditioned,
    Integer defaultCapacity,
    Long categoryId
) {
}