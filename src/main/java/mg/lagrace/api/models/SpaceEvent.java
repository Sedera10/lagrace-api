package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "space_event")
public class SpaceEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_event")
    private Long idEvent;
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdEvent() { return idEvent; }
    public void setIdEvent(Long idEvent) { this.idEvent = idEvent; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
