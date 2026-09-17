package mg.lagrace.api.dto.room;

public record RoomTypeRequest(
    String name,
    String description,
    Boolean isAirConditioned,
    Integer defaultCapacity,
    Long categoryId
) {
}