package mg.lagrace.api.leave.persistence;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import mg.lagrace.api.leave.LeaveRequest;
public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
 @Query(value="select l from LeaveRequest l join fetch l.employee join fetch l.leaveType join fetch l.approvedBy where l.id=:id") Optional<LeaveRequest> details(Long id);
 @Query(value="select l from LeaveRequest l join fetch l.leaveType where l.employee.idEmployee=:employeeId and (:excludeId is null or l.id<>:excludeId) and l.startDate<=:endDate and l.endDate>=:startDate") java.util.List<LeaveRequest> overlapping(Long employeeId, LocalDate startDate, LocalDate endDate, Long excludeId);
 boolean existsByLeaveTypeIdLeaveType(Long leaveTypeId);
}
