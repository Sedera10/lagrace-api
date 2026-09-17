package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByEmployeeIdEmployee(Long employeeId);
}
