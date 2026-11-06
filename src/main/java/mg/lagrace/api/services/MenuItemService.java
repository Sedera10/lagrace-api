package mg.lagrace.api.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.lagrace.api.dto.menu.MenuItemResponse;
import mg.lagrace.api.models.MenuItem;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.repositories.MenuItemRateRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {
    private final MenuItemRateRepository menuItemRateRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRateRepository menuItemRateRepository, MenuItemRepository menuItemRepository) {
        this.menuItemRateRepository = menuItemRateRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public MenuItemRate getApplicableRate(Long menuItemId, LocalDate date) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new IllegalArgumentException("Menu item introuvable");
        }
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        MenuItemRate rate = menuItemRateRepository.findApplicableByMenuItem(menuItemId, effectiveDate);
        if (rate == null) {
            throw new IllegalArgumentException("Aucun tarif applicable trouvé pour ce menu item à cette date");
        }
        return rate;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuItemsWithPrices(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<MenuItem> menuItems = menuItemRepository.findAllWithCategory();
        Map<Long, MenuItemRate> ratesByMenuItemId = new HashMap<>();

        for (MenuItemRate rate : menuItemRateRepository.findAllApplicableByDate(effectiveDate)) {
            ratesByMenuItemId.putIfAbsent(rate.getMenuItem().getIdMenuItem(), rate);
        }

        return menuItems.stream()
                .map(item -> toResponse(item, ratesByMenuItemId.get(item.getIdMenuItem())))
                .toList();
    }

    private MenuItemResponse toResponse(MenuItem item, MenuItemRate rate) {
        return new MenuItemResponse(
                item.getIdMenuItem(),
                item.getCategory().getIdMenuCategory(),
                item.getCategory().getName(),
                item.getCategory().getPosition(),
                item.getName(),
                item.getCode(),
                item.getDescription(),
                item.getActive(),
                item.getCreatedAt(),
                rate != null ? rate.getIdRate() : null,
                rate != null ? rate.getAmount() : null,
                rate != null ? rate.getValidFrom() : null,
                rate != null ? rate.getValidTo() : null);
    }
}
