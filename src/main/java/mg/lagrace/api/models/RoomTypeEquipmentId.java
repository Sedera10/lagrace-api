package mg.lagrace.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoomTypeEquipmentId implements Serializable {

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    public RoomTypeEquipmentId() {}

    public RoomTypeEquipmentId(Long typeId, Long equipmentId) {
        this.typeId = typeId;
        this.equipmentId = equipmentId;
    }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomTypeEquipmentId that)) return false;
        return Objects.equals(typeId, that.typeId)
            && Objects.equals(equipmentId, that.equipmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId, equipmentId);
    }
}