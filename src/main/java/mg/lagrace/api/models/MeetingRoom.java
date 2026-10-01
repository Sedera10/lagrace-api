package mg.lagrace.api.models;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "meeting_room")
public class MeetingRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_meeting_room")
    private Long idMeetingRoom;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getIdMeetingRoom() { return idMeetingRoom; }
    public void setIdMeetingRoom(Long idMeetingRoom) { this.idMeetingRoom = idMeetingRoom; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
