package mg.lagrace.api.customer.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.customer.CustomerTag;

public interface CustomerTagRepository extends JpaRepository<CustomerTag, Integer> {
    List<CustomerTag> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdTagNot(String name, Integer idTag);
}
