package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "room_type_equipment")
public class RoomTypeEquipment {

    @EmbeddedId
    private RoomTypeEquipmentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("typeId")
    @JoinColumn(name = "type_id", nullable = false)
    private RoomType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("equipmentId")
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    public RoomTypeEquipmentId getId() { return id; }
    public void setId(RoomTypeEquipmentId id) { this.id = id; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

}