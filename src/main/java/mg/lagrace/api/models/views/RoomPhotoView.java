package mg.lagrace.api.models.views;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Vue v_room_photo — photos d'une chambre, hors couverture.
 *
 * Clé composite obligatoire : une photo rattachée au TYPE réapparaît pour
 * chaque chambre de ce type, donc idPhoto seul n'est pas unique dans la
 * vue. Sans clé composite, Hibernate dédoublonnerait les lignes par
 * idPhoto et renverrait des résultats faux.
 */
@Entity
@Immutable
@Table(name = "v_room_photo")
public class RoomPhotoView {

    @EmbeddedId
    private RoomPhotoId id;

    @Column(name = "url")
    private String url;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "caption")
    private String caption;

    /** "ROOM" (photo propre à la chambre) ou "TYPE" (photo du type). */
    @Column(name = "scope")
    private String scope;

    protected RoomPhotoView() {
    }

    public RoomPhotoId getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getCaption() {
        return caption;
    }

    public String getScope() {
        return scope;
    }

    @Embeddable
    public static class RoomPhotoId implements Serializable {

        @Column(name = "id_room")
        private Integer idRoom;

        @Column(name = "id_photo")
        private Integer idPhoto;

        protected RoomPhotoId() {
        }

        public Integer getIdRoom() {
            return idRoom;
        }

        public Integer getIdPhoto() {
            return idPhoto;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RoomPhotoId other)) {
                return false;
            }
            return Objects.equals(idRoom, other.idRoom)
                    && Objects.equals(idPhoto, other.idPhoto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idRoom, idPhoto);
        }
    }
}
