package mg.lagrace.api.catalog.room.dto.room.equipment;

import mg.lagrace.api.catalog.room.Equipment;
import mg.lagrace.api.catalog.room.Room;

public record AddRoomTypeEquipmentRequest(
    Long equipmentId,
    Integer quantity
) {}
