package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.Job;

public interface JobRepository extends JpaRepository<Job, Long> {
    boolean existsByDepartmentIdDepartmentAndName(Long departmentId, String name);
    boolean existsByDepartmentIdDepartmentAndNameAndIdJobNot(Long departmentId, String name, Long idJob);
}
