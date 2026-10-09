package mg.lagrace.api.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.lagrace.api.models.enums.PhotoItemType;

@Entity
@Table(name = "photo")
@Getter
@Setter
@NoArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_photo")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private PhotoItemType itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "public_id", nullable = false, unique = true, length = 500)
    private String publicId;

    private String caption;

    @Column(name = "alt_text")
    private String altText;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(name = "is_cover", nullable = false)
    private boolean cover;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
