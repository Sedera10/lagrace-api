package mg.lagrace.api.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByUserIdUser(Long userId);
    Optional<Employee> findByUserIdUser(Long userId);
}
