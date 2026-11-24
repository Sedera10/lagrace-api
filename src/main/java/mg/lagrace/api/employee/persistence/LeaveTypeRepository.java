package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.LeaveType;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    boolean existsByName(String name);
}
