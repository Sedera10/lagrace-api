package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.RoomRate;

public interface RoomRateRepository extends JpaRepository<RoomRate,Long> {
    List<RoomRate> findByType_IdTypeAndUnit_Service(Long typeId, String service);
    Optional<RoomRate> findByType_IdTypeAndUnit_IdUnitAndValidFrom(Long typeId, Long unitId, LocalDate validFrom);

    // Tous les tarifs d'un type (affichage). Le JOIN FETCH évite le N+1 sur unit.
    @Query("""
        SELECT r FROM RoomRate r
        JOIN FETCH r.unit u
        WHERE r.type.idType = :typeId
        ORDER BY u.idUnit, r.validFrom DESC
        """)
    List<RoomRate> findAllByType(@Param("typeId") Long typeId);

    // Un seul tarif applicable par unité à la date donnée
    @Query(value = """
        SELECT DISTINCT ON (unit_id) *
        FROM room_rate
        WHERE type_id = :typeId
          AND valid_from <= :date
          AND (valid_to IS NULL OR valid_to >= :date)
        ORDER BY unit_id,
                 (valid_to IS NULL),
                 valid_from DESC,
                 id_rate DESC
        """, nativeQuery = true)
    List<RoomRate> findApplicableByType(@Param("typeId") Long typeId,
                                        @Param("date") LocalDate date);

    // ======  VERIFICATION  =========
    // Deux tarifs qui démarrent le même jour (même type + unité)
    boolean existsByType_IdTypeAndUnit_IdUnitAndValidFromAndIdRateNot(
            Long typeId, Long unitId, LocalDate validFrom, Long excludeId);

    // Un autre tarif permanent existe déjà
    boolean existsByType_IdTypeAndUnit_IdUnitAndValidToIsNullAndIdRateNot(
            Long typeId, Long unitId, Long excludeId);

    // Chevauchement avec une autre promotion (période bornée)
    @Query("""
        SELECT COUNT(r) > 0 FROM RoomRate r
        WHERE r.type.idType = :typeId
          AND r.unit.idUnit = :unitId
          AND r.idRate <> :excludeId
          AND r.validTo IS NOT NULL
          AND r.validFrom <= :to
          AND r.validTo >= :from
        """)
    boolean existsOverlappingPromo(@Param("typeId") Long typeId,
                                   @Param("unitId") Long unitId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("excludeId") Long excludeId);
}
