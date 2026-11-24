package mg.lagrace.api.customer.persistence;

import mg.lagrace.api.customer.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.customer.IdentificationDocument;

public interface IdentificationDocRepository extends JpaRepository<IdentificationDocument,Long> {
    boolean existsByName(String name);
}
