package mg.lagrace.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_type")
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_leave_type")
    private Long idLeaveType;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Long getIdLeaveType() { return idLeaveType; }
    public void setIdLeaveType(Long idLeaveType) { this.idLeaveType = idLeaveType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
