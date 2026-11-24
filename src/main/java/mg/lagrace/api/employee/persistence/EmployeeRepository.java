package mg.lagrace.api.employee.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByUserIdUser(Long userId);
    Optional<Employee> findByUserIdUser(Long userId);
}
