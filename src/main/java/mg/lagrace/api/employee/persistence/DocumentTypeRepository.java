package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.DocumentType;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    boolean existsByName(String name);
}
