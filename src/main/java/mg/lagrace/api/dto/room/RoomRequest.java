package mg.lagrace.api.dto.room;

public record RoomRequest (
    String refRoom,
    Integer typeId,
    String zone,
    Integer floor,
    Integer statusId
) {
}