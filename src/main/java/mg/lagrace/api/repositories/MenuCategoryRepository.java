package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    boolean existsByName(String name);
    List<MenuCategory> findAllByOrderByPositionAscNameAsc();
}
