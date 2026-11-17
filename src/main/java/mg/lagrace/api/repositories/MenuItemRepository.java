package mg.lagrace.api.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import mg.lagrace.api.models.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByCode(String code);
    boolean existsByCategory_IdMenuCategoryAndName(Long categoryId, String name);
    boolean existsByCategory_IdMenuCategory(Long categoryId);

    @Query("SELECT item FROM MenuItem item JOIN FETCH item.category ORDER BY item.category.position, item.name")
    List<MenuItem> findAllWithCategory();
}
