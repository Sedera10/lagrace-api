package mg.lagrace.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.lagrace.api.models.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByDocument_IdDocumentAndIdentificationNumber(
			Long documentId, String identificationNumber);

	@Query("""
		SELECT customer FROM Customer customer
		LEFT JOIN FETCH customer.document
		LEFT JOIN FETCH customer.nationality
		WHERE customer.active = true AND customer.mergedInto IS NULL
			AND customer.idCustomer <> :walkInCustomerId
			AND (
				LOWER(CONCAT(CONCAT(COALESCE(customer.lastName, ''), ' '), COALESCE(customer.firstName, '')))
				LIKE LOWER(CONCAT('%', :term, '%'))
			OR LOWER(CONCAT(CONCAT(COALESCE(customer.firstName, ''), ' '), COALESCE(customer.lastName, '')))
				LIKE LOWER(CONCAT('%', :term, '%'))
			OR LOWER(COALESCE(customer.phone, '')) LIKE LOWER(CONCAT('%', :term, '%'))
			OR LOWER(COALESCE(customer.email, '')) LIKE LOWER(CONCAT('%', :term, '%'))
			OR LOWER(COALESCE(customer.identificationNumber, '')) LIKE LOWER(CONCAT('%', :term, '%')))
		ORDER BY customer.lastName, customer.firstName, customer.idCustomer
		""")
	List<Customer> searchForAutocomplete(@Param("term") String term,
			@Param("walkInCustomerId") Long walkInCustomerId, Pageable pageable);
}
