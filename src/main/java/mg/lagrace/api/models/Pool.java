package mg.lagrace.api.models;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "pool")
public class Pool {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pool", columnDefinition = "integer")
    private Long idPool;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
    }


    public Long getIdPool() { return idPool; }
    public void setIdPool(Long idPool) { this.idPool = idPool; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}