package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.Job;

public interface JobRepository extends JpaRepository<Job, Long> {
    boolean existsByDepartmentIdDepartmentAndName(Long departmentId, String name);
    boolean existsByDepartmentIdDepartmentAndNameAndIdJobNot(Long departmentId, String name, Long idJob);
}
