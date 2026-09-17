package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.RoomCategory;

public interface RoomCategoryRepository extends JpaRepository<RoomCategory, Long> {
    boolean existsByName(String name);
}