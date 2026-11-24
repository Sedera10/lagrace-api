package mg.lagrace.api.catalog.additionalservice.persistence;

import mg.lagrace.api.catalog.space.Space;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import mg.lagrace.api.catalog.additionalservice.AdditionalService;

import java.util.List;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByName(String name);

    @Query("SELECT service FROM AdditionalService service WHERE service.scope.code = 'SPACE' AND service.deposit = true AND service.active = true")
    List<AdditionalService> findActiveSpaceDeposits();
}
