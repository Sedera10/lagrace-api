package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.ContractType;

public interface ContractTypeRepository extends JpaRepository<ContractType, Long> {
    boolean existsByName(String name);
}
