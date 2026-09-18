package mg.lagrace.api.models.views;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Vue v_room_detail — lecture seule.
 *
 * @Immutable indique à Hibernate de ne jamais tenter d'écrire dans cette
 * table : une vue avec des jointures n'est pas modifiable en PostgreSQL.
 * Les écritures passent par les entités Room / RoomType / RoomRate.
 */
@Entity
@Immutable
@Table(name = "v_room_detail")
public class RoomDetail {

    @Id
    @Column(name = "id_room")
    private Integer idRoom;

    @Column(name = "ref_room")
    private String refRoom;

    /** NULL pour les bungalows (pas d'étage). */
    @Column(name = "floor")
    private Integer floor;

    @Column(name = "zone")
    private String zone;

    @Column(name = "description")
    private String description;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status")
    private String status;

    /** Code couleur hexadécimal du statut, ex. "#1D9E75". */
    @Column(name = "color")
    private String color;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "type")
    private String type;

    @Column(name = "default_capacity")
    private Integer defaultCapacity;

    @Column(name = "is_air_conditioned")
    private Boolean airConditioned;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category")
    private String category;

    @Column(name = "cover_photo_id")
    private Integer coverPhotoId;

    /** URL de la photo de couverture ; peut être null si aucune photo. */
    @Column(name = "cover")
    private String cover;

    @Column(name = "rate_id")
    private Integer rateId;

    /** Tarif en vigueur aujourd'hui ; null si aucun tarif actif. */
    @Column(name = "rate")
    private BigDecimal rate;

    /** Unité du tarif : "Nuit", "Forfait", etc. */
    @Column(name = "rate_unit")
    private String rateUnit;

    protected RoomDetail() {
    }

    // Getters & Setters
    public Integer getIdRoom() { return idRoom; }
    public String getRefRoom() { return refRoom; }
    public Integer getFloor() { return floor; }
    public String getZone() { return zone; }
    public String getDescription() { return description; }
    public Integer getStatusId() { return statusId; }
    public String getStatus() { return status; }
    public String getColor() { return color; }
    public Integer getTypeId() { return typeId; }
    public String getType() { return type; }
    public Integer getDefaultCapacity() { return defaultCapacity; }
    public Boolean getAirConditioned() { return airConditioned; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategory() { return category; }
    public Integer getCoverPhotoId() { return coverPhotoId; }
    public String getCover() { return cover; }
    public Integer getRateId() { return rateId; }
    public BigDecimal getRate() { return rate; }
    public String getRateUnit() { return rateUnit; }
}
