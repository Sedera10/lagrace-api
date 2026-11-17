package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import mg.lagrace.api.models.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByName(String name);

    @Query("SELECT service FROM AdditionalService service WHERE service.scope.code = 'SPACE' AND service.deposit = true AND service.active = true")
    List<AdditionalService> findActiveSpaceDeposits();
}
