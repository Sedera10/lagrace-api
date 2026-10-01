package mg.lagrace.api.dto.room.equipment;

public record AddRoomTypeEquipmentRequest(
    Long equipmentId,
    Integer quantity
) {}
