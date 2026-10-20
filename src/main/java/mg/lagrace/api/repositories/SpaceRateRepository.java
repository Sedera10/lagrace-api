package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.SpaceRate;

public interface SpaceRateRepository extends JpaRepository<SpaceRate, Long> {
    List<SpaceRate> findAllBySpace_IdSpace(Long idSpace);
    List<SpaceRate> findAllBySpace(Space space);

    // Tous les tarifs d'un espace (pour l'affichage)
    List<SpaceRate> findBySpace_IdSpaceOrderByUnit_IdUnitAscMinPersonAscValidFromDesc(Long spaceId);

    // Un seul tarif applicable par créneau (unité + min + max) à la date donnée
    @Query(value = """
        SELECT DISTINCT ON (unit_id, min_persons, max_persons) *
        FROM space_rate
        WHERE space_id = :spaceId
          AND valid_from <= :date
          AND (valid_to IS NULL OR valid_to >= :date)
        ORDER BY unit_id, min_persons, max_persons,
                 (valid_to IS NULL),
                 valid_from DESC,
                 id_rate DESC
        """, nativeQuery = true)
    List<SpaceRate> findApplicableBySpace(@Param("spaceId") Long spaceId,
                                          @Param("date") LocalDate date);
}
