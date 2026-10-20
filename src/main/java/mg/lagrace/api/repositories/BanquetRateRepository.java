package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.BanquetMenuType;
import mg.lagrace.api.models.BanquetRate;

public interface BanquetRateRepository extends JpaRepository<BanquetRate, Long> {
    List<BanquetRate> findAllByMenuType_IdMenuType(Long idMenuType);
    List<BanquetRate> findAllByMenuType(BanquetMenuType menuType);

    List<BanquetRate> findAllByOrderByMenuType_IdMenuTypeAscMinPersonsAscValidFromDesc();

    @Query(value = """
        SELECT DISTINCT ON (min_persons, max_persons) *
        FROM banquet_rate
        WHERE menu_type_id = CAST(:menuTypeId AS integer)
          AND valid_from <= :date
          AND (valid_to IS NULL OR valid_to >= :date)
        ORDER BY min_persons, max_persons,
                 (valid_to IS NULL),
                 valid_from DESC,
                 id_rate DESC
        """, nativeQuery = true)
    List<BanquetRate> findApplicableByMenuType(@Param("menuTypeId") Long menuTypeId,
                                               @Param("date") LocalDate date);
}
