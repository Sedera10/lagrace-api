package mg.lagrace.api.catalog.restaurant.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.restaurant.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    boolean existsByName(String name);
    List<MenuCategory> findAllByOrderByPositionAscNameAsc();
}
