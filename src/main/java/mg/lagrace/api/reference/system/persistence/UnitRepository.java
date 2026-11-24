package mg.lagrace.api.reference.system.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.Unit;

import java.util.List;


public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByName(String name);

    List<Unit> findByService(String service);
}
