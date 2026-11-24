package mg.lagrace.api.leave;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.employee.LeaveType;

@Entity
@Table(name = "leave_request")
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_leave_request") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id") private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "leave_type_id") private LeaveType leaveType;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by_user_id") private User approvedBy;
    @Column(name = "notes") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId(){return id;} public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;} public LeaveType getLeaveType(){return leaveType;} public void setLeaveType(LeaveType v){leaveType=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public User getApprovedBy(){return approvedBy;} public void setApprovedBy(User v){approvedBy=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
