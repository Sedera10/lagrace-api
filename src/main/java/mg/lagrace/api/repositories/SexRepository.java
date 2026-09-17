package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Sex;

public interface SexRepository extends JpaRepository<Sex,Long> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
