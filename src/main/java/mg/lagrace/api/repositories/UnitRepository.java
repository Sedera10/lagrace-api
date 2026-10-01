package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Unit;
import java.util.List;


public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByName(String name);

    List<Unit> findByService(String service);
}
