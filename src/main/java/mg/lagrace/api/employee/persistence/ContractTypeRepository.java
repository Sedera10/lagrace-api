package mg.lagrace.api.employee.persistence;

import mg.lagrace.api.employee.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.employee.ContractType;

public interface ContractTypeRepository extends JpaRepository<ContractType, Long> {
    boolean existsByName(String name);
}
