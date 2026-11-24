package mg.lagrace.api.reference.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.Sex;

public interface SexRepository extends JpaRepository<Sex,Long> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
