package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);
}
