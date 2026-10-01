package mg.lagrace.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.RoomTypeEquipment;
import mg.lagrace.api.models.RoomTypeEquipmentId;

public interface RoomTypeEquipmentRepository
        extends JpaRepository<RoomTypeEquipment, RoomTypeEquipmentId> {

    List<RoomTypeEquipment> findByType_IdTypeOrderByEquipment_NameAsc(Long typeId);

    Optional<RoomTypeEquipment> findByType_IdTypeAndEquipment_IdEquipment(Long typeId, Long equipmentId);

    boolean existsByType_IdTypeAndEquipment_IdEquipment(Long typeId, Long equipmentId);

    void deleteByType_IdTypeAndEquipment_IdEquipment(Long typeId, Long equipmentId);
}
