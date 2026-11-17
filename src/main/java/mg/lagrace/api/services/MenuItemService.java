package mg.lagrace.api.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import mg.lagrace.api.dto.menu.MenuItemRequest;
import mg.lagrace.api.dto.menu.MenuItemResponse;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.MenuItem;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.repositories.MenuCategoryRepository;
import mg.lagrace.api.repositories.MenuItemRateRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {
    private final MenuItemRateRepository menuItemRateRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuItemService(MenuItemRateRepository menuItemRateRepository, MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository) {
        this.menuItemRateRepository = menuItemRateRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
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

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItem(Long id, LocalDate date) {
        MenuItem item = menuItemRepository.findById(id).orElseThrow(() -> missing(id));
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return toResponse(item, menuItemRateRepository.findApplicableByMenuItem(id, effectiveDate));
    }

    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        validate(request);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();
        if (menuItemRepository.existsByCode(code)
                || menuItemRepository.existsByCategory_IdMenuCategoryAndName(request.categoryId(), name)) {
            throw new DuplicateResourceException("Le menu item existe déjà.");
        }
        MenuItem item = new MenuItem();
        apply(item, request, name, code);
        return toResponse(menuItemRepository.save(item), null);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        validate(request);
        MenuItem item = menuItemRepository.findById(id).orElseThrow(() -> missing(id));
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String name = request.name().trim();
        boolean categoryOrNameChanged = !item.getCategory().getIdMenuCategory().equals(request.categoryId())
                || !item.getName().equals(name);
        if ((!item.getCode().equals(code) && menuItemRepository.existsByCode(code))
                || (categoryOrNameChanged
                    && menuItemRepository.existsByCategory_IdMenuCategoryAndName(request.categoryId(), name))) {
            throw new DuplicateResourceException("Le menu item existe déjà.");
        }
        apply(item, request, name, code);
        return toResponse(menuItemRepository.save(item), null);
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id).orElseThrow(() -> missing(id));
        if (menuItemRateRepository.existsByMenuItem_IdMenuItem(id)) {
            throw new ValidationException("Ce menu item possède des tarifs et ne peut pas être supprimé.");
        }
        menuItemRepository.delete(item);
    }

    private void validate(MenuItemRequest request) {
        if (request == null) throw new ValidationException("Les données du menu item sont obligatoires.");
        if (request.categoryId() == null) throw new ValidationException("La catégorie est obligatoire.");
        if (request.name() == null || request.name().isBlank()) throw new ValidationException("Le nom du menu item est obligatoire.");
        if (request.code() == null || request.code().isBlank()) throw new ValidationException("Le code du menu item est obligatoire.");
        if (!menuCategoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Catégorie de menu introuvable avec l'id " + request.categoryId());
        }
    }

    private void apply(MenuItem item, MenuItemRequest request, String name, String code) {
        item.setCategory(menuCategoryRepository.getReferenceById(request.categoryId()));
        item.setName(name);
        item.setCode(code);
        item.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        item.setActive(request.active() == null || request.active());
    }

    private ResourceNotFoundException missing(Long id) {
        return new ResourceNotFoundException("Menu item introuvable avec l'id " + id);
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
