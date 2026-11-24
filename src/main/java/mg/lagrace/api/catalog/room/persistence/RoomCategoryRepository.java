package mg.lagrace.api.catalog.room.persistence;

import mg.lagrace.api.catalog.room.Room;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.room.RoomCategory;

public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    boolean existsByName(String name);
}