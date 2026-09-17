package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "room_photo")
public class RoomPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_photo")
    private Long idPhoto;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "caption", length = 255)
    private String caption;

    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = false;

    // Une photo appartient soit à une chambre...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // ...soit à un type de chambre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private RoomType type;

    public Long getIdPhoto() { return idPhoto; }
    public void setIdPhoto(Long idPhoto) { this.idPhoto = idPhoto; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Boolean getIsCover() { return isCover; }
    public void setIsCover(Boolean isCover) { this.isCover = isCover; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

}
