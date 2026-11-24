package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByEmployeeIdEmployee(Long employeeId);
}
