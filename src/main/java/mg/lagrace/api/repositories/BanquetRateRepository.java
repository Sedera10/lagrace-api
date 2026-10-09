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

        @Query("""
                        SELECT rate
                        FROM BanquetRate rate
                        WHERE rate.validFrom <= :date
                            AND (rate.validTo IS NULL OR rate.validTo >= :date)
                        ORDER BY rate.menuType.idMenuType, rate.minPersons
                        """)
        List<BanquetRate> findAllCurrentAt(@Param("date") LocalDate date);

        @Query("""
                SELECT rate
                FROM BanquetRate rate
                WHERE rate.validTo < :date
                   OR rate.validFrom > :date
                ORDER BY rate.menuType.idMenuType, rate.validFrom DESC, rate.minPersons
                """)
        List<BanquetRate> findAllOtherThanCurrentAt(@Param("date") LocalDate date);
}
