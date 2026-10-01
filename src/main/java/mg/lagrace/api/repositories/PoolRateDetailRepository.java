package mg.lagrace.api.repositories;


import mg.lagrace.api.models.views.PoolRateDetail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PoolRateDetailRepository extends JpaRepository<PoolRateDetail, Long> {

    List<PoolRateDetail> findByPoolId(Long poolId);

    /**
     * Tarifs réellement applicables aujourd'hui.
     *
     * Règle :
     * - un tarif temporaire valide est prioritaire
     * - sinon le tarif permanent valide est utilisé
     */
    @Query(value = """
        SELECT DISTINCT ON (
            r.id_service,
            r.id_unit,
            r.min_age,
            r.max_age,
            r.meal_included
        )
        r.*
        FROM v_pool_rate r
        WHERE r.pool_id = :poolId
          AND r.valid_from <= :date
          AND (r.valid_to IS NULL OR r.valid_to >= :date)
        ORDER BY
            r.id_service,
            r.id_unit,
            r.min_age,
            r.max_age,
            r.meal_included,
            r.is_temporary DESC,
            r.valid_from DESC,
            r.id_rate DESC
        """,
        nativeQuery = true)
    List<PoolRateDetail> findApplicableRates(
            @Param("poolId") Long poolId,
            @Param("date") LocalDate date
    );
}
