package mg.lagrace.api.catalog.restaurant.persistence;

import mg.lagrace.api.catalog.restaurant.MenuItem;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.catalog.restaurant.MenuItemRate;

public interface MenuItemRateRepository extends JpaRepository<MenuItemRate, Long> {
    List<MenuItemRate> findAllByMenuItem_IdMenuItemOrderByValidFromDesc(Long menuItemId);
  boolean existsByMenuItem_IdMenuItem(Long menuItemId);

    // Un seul tarif applicable à la date donnée pour un menu item
    @Query(value = """
        SELECT * FROM menu_item_rate
        WHERE menu_item_id = :menuItemId
          AND valid_from <= :date
          AND (valid_to IS NULL OR valid_to >= :date)
        ORDER BY (valid_to IS NULL) DESC, valid_from DESC, id_rate DESC
        LIMIT 1
        """, nativeQuery = true)
    MenuItemRate findApplicableByMenuItem(@Param("menuItemId") Long menuItemId,
                                          @Param("date") LocalDate date);

    @Query("""
        SELECT rate FROM MenuItemRate rate
        JOIN FETCH rate.menuItem item
        WHERE rate.validFrom <= :date
          AND (rate.validTo IS NULL OR rate.validTo >= :date)
        ORDER BY item.idMenuItem, rate.validFrom DESC, rate.idRate DESC
        """)
    List<MenuItemRate> findAllApplicableByDate(@Param("date") LocalDate date);
}
