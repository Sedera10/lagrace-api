package mg.lagrace.api.catalog.room.dto.room;

import mg.lagrace.api.catalog.room.Room;

public record RoomRequest (
    String refRoom,
    Integer typeId,
    String zone,
    Integer floor,
    Integer statusId
) {
}