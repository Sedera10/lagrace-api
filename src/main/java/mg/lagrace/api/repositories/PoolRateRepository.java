package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.PoolRate;

public interface PoolRateRepository extends JpaRepository<PoolRate, Long> {
    List<PoolRate> findByPool_IdPoolOrderByService_IdServiceAscUnit_IdUnitAscValidFromDesc(Long poolId);

    @Query(value = """
        SELECT DISTINCT ON (
            pr.unit_id,
            pr.meal_included,
            pr.min_age,
            pr.max_age
        ) pr.*
        FROM pool_rate pr
        WHERE pr.pool_id = CAST(:poolId AS integer)
          AND pr.service_id = CAST(:serviceId AS integer)
          AND pr.valid_from <= :date
          AND (pr.valid_to IS NULL OR pr.valid_to >= :date)
        ORDER BY
            pr.unit_id,
            pr.meal_included,
            pr.min_age,
            pr.max_age,
            (pr.valid_to IS NULL),
            pr.valid_from DESC,
            pr.id_rate DESC
        """, nativeQuery = true)
    List<PoolRate> findApplicableByPoolAndService(@Param("poolId") Long poolId,
                                                  @Param("serviceId") Long serviceId,
                                                  @Param("date") LocalDate date);
}
