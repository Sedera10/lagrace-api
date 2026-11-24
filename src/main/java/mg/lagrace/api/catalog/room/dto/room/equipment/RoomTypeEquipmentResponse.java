package mg.lagrace.api.catalog.room.dto.room.equipment;

import mg.lagrace.api.catalog.room.Equipment;
import mg.lagrace.api.catalog.room.Room;

import mg.lagrace.api.catalog.room.RoomTypeEquipment;

public record RoomTypeEquipmentResponse(
        Long typeId,
        Long equipmentId,
        String name,
        String description,
        Integer quantity
) {
    public static RoomTypeEquipmentResponse from(RoomTypeEquipment item) {
        return new RoomTypeEquipmentResponse(
                item.getType() != null ? item.getType().getIdType() : null,
                item.getEquipment() != null ? item.getEquipment().getIdEquipment() : null,
                item.getEquipment() != null ? item.getEquipment().getName() : null,
                item.getEquipment() != null ? item.getEquipment().getDescription() : null,
                item.getQuantity()
        );
    }
}
