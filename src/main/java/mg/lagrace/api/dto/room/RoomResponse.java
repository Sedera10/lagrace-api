package mg.lagrace.api.dto.room;

import java.math.BigDecimal;

public record RoomResponse (
    Long idRoom,
    String refRoom,
    String zone,
    String description,
    Integer statusId,
    String status,
    String color,
    Integer typeId,
    String type,
    Integer defaultCapacity,
    Boolean airConditioned,
    Integer categoryId,
    String  category,
    Integer coverPhotoId,
    String cover,
    Integer rateId,
    BigDecimal rate,
    String rateUnit
) {
}
