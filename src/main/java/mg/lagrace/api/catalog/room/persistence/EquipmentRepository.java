package mg.lagrace.api.catalog.room.persistence;

import mg.lagrace.api.catalog.room.Room;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.room.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByName(String name);
}
