package mg.lagrace.api.models;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "space")
public class Space {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_space")
    private Long idSpace;
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "capacity", nullable = false)
    private Integer capacity;
    @Column(name = "created_at", nullable = true, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getIdSpace() { return idSpace; }
    public void setIdSpace(Long idSpace) { this.idSpace = idSpace; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}