package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.RoomRate;

public interface RoomRateRepository extends JpaRepository<RoomRate,Long> {
    List<RoomRate> findByType_IdTypeAndUnit_Service(Long typeId, String service);
    Optional<RoomRate> findByType_IdTypeAndUnit_IdUnitAndValidFrom(Long typeId, Long unitId, LocalDate validFrom);
}
