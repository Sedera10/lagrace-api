package mg.lagrace.api.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_type")
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type")
    private Long idType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private RoomCategory category;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_air_conditioned", nullable = false)
    private Boolean isAirConditioned = false;

    @Column(name = "default_capacity", nullable = false)
    private Integer defaultCapacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomTypeEquipment> equipments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getIdType() { return idType; }
    public void setIdType(Long idType) { this.idType = idType; }

    public RoomCategory getCategory() { return category; }
    public void setCategory(RoomCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsAirConditioned() { return isAirConditioned; }
    public void setIsAirConditioned(Boolean isAirConditioned) { this.isAirConditioned = isAirConditioned; }

    public Integer getDefaultCapacity() { return defaultCapacity; }
    public void setDefaultCapacity(Integer defaultCapacity) { this.defaultCapacity = defaultCapacity; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<RoomTypeEquipment> getEquipments() { return equipments; }
    public void setEquipments(List<RoomTypeEquipment> equipments) { this.equipments = equipments; }

    // Helper pratique
    public void addEquipment(Equipment equipment, int quantity) {
        RoomTypeEquipment rte = new RoomTypeEquipment();
        rte.setId(new RoomTypeEquipmentId(this.idType, equipment.getIdEquipment()));
        rte.setType(this);
        rte.setEquipment(equipment);
        rte.setQuantity(quantity);
        equipments.add(rte);
    }

    public void removeEquipment(Equipment equipment) {
        equipments.removeIf(rte -> rte.getEquipment().getIdEquipment().equals(equipment.getIdEquipment()));
    }
}
