package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import mg.lagrace.api.dto.service.AdditionalServiceRequest;
import mg.lagrace.api.dto.service.MenuCategoryRequest;
import mg.lagrace.api.dto.service.ServiceRequest;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.MeetingRoomService;
import mg.lagrace.api.models.MenuCategory;
import mg.lagrace.api.models.PoolService;
import mg.lagrace.api.models.ServiceScope;
import mg.lagrace.api.models.SpaceEvent;
import mg.lagrace.api.models.Unit;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.MeetingRoomServiceRepository;
import mg.lagrace.api.repositories.MenuCategoryRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import mg.lagrace.api.repositories.PoolServiceRepository;
import mg.lagrace.api.repositories.ServiceScopeRepository;
import mg.lagrace.api.repositories.SpaceEventRepository;
import mg.lagrace.api.repositories.UnitRepository;

@Service
public class ServiceConfigService {
    private final SpaceEventRepository events;
    private final MeetingRoomServiceRepository meetingServices;
    private final PoolServiceRepository poolServices;
    private final AdditionalServiceRepository additionalServices;
    private final UnitRepository units;
    private final ServiceScopeRepository scopes;
    private final MenuCategoryRepository menuCategories;
    private final MenuItemRepository menuItems;

    public ServiceConfigService(SpaceEventRepository events, MeetingRoomServiceRepository meetingServices,
            PoolServiceRepository poolServices, AdditionalServiceRepository additionalServices,
            UnitRepository units, ServiceScopeRepository scopes, MenuCategoryRepository menuCategories,
            MenuItemRepository menuItems) {
        this.events = events; this.meetingServices = meetingServices; this.poolServices = poolServices;
        this.additionalServices = additionalServices; this.units = units; this.scopes = scopes;
        this.menuCategories = menuCategories; this.menuItems = menuItems;
    }

    public List<SpaceEvent> getSpaceEvents() { return events.findAll(); }
    public SpaceEvent addSpaceEvent(String value) { SpaceEvent e = new SpaceEvent(); e.setName(uniqueName(value, events.existsByName(value))); return events.save(e); }
    public SpaceEvent updateSpaceEvent(Long id, String value) { SpaceEvent e = events.findById(id).orElseThrow(() -> missing("Événement", id)); String name = clean(value, "de l'événement"); if (!e.getName().equals(name) && events.existsByName(name)) duplicate(); e.setName(name); return events.save(e); }
    public void deleteSpaceEvent(Long id) { events.delete(events.findById(id).orElseThrow(() -> missing("Événement", id))); }

    public List<MeetingRoomService> getMeetingRoomServices() { return meetingServices.findAll(); }
    public MeetingRoomService addMeetingRoomService(ServiceRequest r) { validateService(r); if (meetingServices.existsByCode(r.code().trim()) || meetingServices.existsByName(r.name().trim())) duplicate(); MeetingRoomService s = new MeetingRoomService(); apply(s, r); return meetingServices.save(s); }
    public MeetingRoomService updateMeetingRoomService(Long id, ServiceRequest r) { validateService(r); MeetingRoomService s = meetingServices.findById(id).orElseThrow(() -> missing("Service salle de réunion", id)); String code = r.code().trim(), name = r.name().trim(); if ((!s.getCode().equals(code) && meetingServices.existsByCode(code)) || (!s.getName().equals(name) && meetingServices.existsByName(name))) duplicate(); apply(s, r); return meetingServices.save(s); }
    public void deleteMeetingRoomService(Long id) { meetingServices.delete(meetingServices.findById(id).orElseThrow(() -> missing("Service salle de réunion", id))); }

    public List<PoolService> getPoolServices() { return poolServices.findAll(); }
    public PoolService addPoolService(ServiceRequest r) { validateService(r); if (poolServices.existsByCode(r.code().trim()) || poolServices.existsByName(r.name().trim())) duplicate(); PoolService s = new PoolService(); apply(s, r); return poolServices.save(s); }
    public PoolService updatePoolService(Long id, ServiceRequest r) { validateService(r); PoolService s = poolServices.findById(id).orElseThrow(() -> missing("Service piscine", id)); String code = r.code().trim(), name = r.name().trim(); if ((!s.getCode().equals(code) && poolServices.existsByCode(code)) || (!s.getName().equals(name) && poolServices.existsByName(name))) duplicate(); apply(s, r); return poolServices.save(s); }
    public void deletePoolService(Long id) { poolServices.delete(poolServices.findById(id).orElseThrow(() -> missing("Service piscine", id))); }

    public List<ServiceScope> getScopes() { return scopes.findAll(); }
    public ServiceScope addScope(String value) { ServiceScope s = new ServiceScope(); s.setCode(uniqueCode(value, scopes.existsByCode(value))); return scopes.save(s); }
    public ServiceScope updateScope(Long id, String value) { ServiceScope s = scopes.findById(id).orElseThrow(() -> missing("Périmètre", id)); String code = clean(value, "du périmètre").toUpperCase(); if (!s.getCode().equals(code) && scopes.existsByCode(code)) duplicate(); s.setCode(code); return scopes.save(s); }
    public void deleteScope(Long id) { scopes.delete(scopes.findById(id).orElseThrow(() -> missing("Périmètre", id))); }

    public List<Unit> getUnits() { return units.findAll(); }
    public Unit addUnit(String value) { Unit u = new Unit(); u.setName(uniqueName(value, units.existsByName(value))); return units.save(u); }
    public Unit updateUnit(Long id, String value) { Unit u = units.findById(id).orElseThrow(() -> missing("Unité", id)); String name = clean(value, "de l'unité"); if (!u.getName().equals(name) && units.existsByName(name)) duplicate(); u.setName(name); return units.save(u); }
    public void deleteUnit(Long id) { units.delete(units.findById(id).orElseThrow(() -> missing("Unité", id))); }

    public List<AdditionalService> getAdditionalServices() { return additionalServices.findAll(); }
    public AdditionalService addAdditionalService(AdditionalServiceRequest r) { validateAdditional(r); if (additionalServices.existsByName(r.name().trim())) duplicate(); validateSpaceDeposit(r, null); return additionalServices.save(apply(new AdditionalService(), r)); }
    public AdditionalService updateAdditionalService(Long id, AdditionalServiceRequest r) { validateAdditional(r); AdditionalService s = additionalServices.findById(id).orElseThrow(() -> missing("Service additionnel", id)); if (!s.getName().equals(r.name().trim()) && additionalServices.existsByName(r.name().trim())) duplicate(); validateSpaceDeposit(r, id); return additionalServices.save(apply(s, r)); }
    public void deleteAdditionalService(Long id) { additionalServices.delete(additionalServices.findById(id).orElseThrow(() -> missing("Service additionnel", id))); }

    public List<MenuCategory> getMenuCategories() { return menuCategories.findAllByOrderByPositionAscNameAsc(); }
    public MenuCategory getMenuCategory(Long id) { return menuCategories.findById(id).orElseThrow(() -> missing("Catégorie de menu", id)); }
    public MenuCategory addMenuCategory(MenuCategoryRequest request) {
        validateMenuCategory(request);
        String name = request.name().trim();
        if (menuCategories.existsByName(name)) duplicate();
        MenuCategory category = new MenuCategory();
        apply(category, request, name);
        return menuCategories.save(category);
    }
    public MenuCategory updateMenuCategory(Long id, MenuCategoryRequest request) {
        validateMenuCategory(request);
        MenuCategory category = menuCategories.findById(id).orElseThrow(() -> missing("Catégorie de menu", id));
        String name = request.name().trim();
        if (!category.getName().equals(name) && menuCategories.existsByName(name)) duplicate();
        apply(category, request, name);
        return menuCategories.save(category);
    }
    public void deleteMenuCategory(Long id) {
        MenuCategory category = menuCategories.findById(id).orElseThrow(() -> missing("Catégorie de menu", id));
        if (menuItems.existsByCategory_IdMenuCategory(id)) {
            throw new ValidationException("Cette catégorie contient des menu items et ne peut pas être supprimée.");
        }
        menuCategories.delete(category);
    }

    private AdditionalService apply(AdditionalService s, AdditionalServiceRequest r) { s.setName(r.name().trim()); s.setScope(scopes.findById(r.scopeId()).orElseThrow(() -> missing("Périmètre", r.scopeId()))); s.setUnit(units.findById(r.unitId()).orElseThrow(() -> missing("Unité", r.unitId()))); s.setPrice(r.price()); s.setDeposit(Boolean.TRUE.equals(r.deposit())); s.setCateringCondition(r.cateringCondition()); s.setFreeWithCaterer(Boolean.TRUE.equals(r.freeWithCaterer())); s.setActive(r.active() == null || r.active()); return s; }
    private void validateMenuCategory(MenuCategoryRequest request) { if (request == null) throw new ValidationException("Les données de la catégorie sont obligatoires."); clean(request.name(), "de la catégorie"); if (request.position() != null && request.position() < 0) throw new ValidationException("La position doit être positive ou nulle."); }
    private void apply(MenuCategory category, MenuCategoryRequest request, String name) { category.setName(name); category.setPosition(request.position() == null ? 0 : request.position()); }
    private void validateAdditional(AdditionalServiceRequest r) { if (r == null) throw new ValidationException("Les données du service sont obligatoires."); clean(r.name(), "du service"); if (r.scopeId() == null || r.unitId() == null) throw new ValidationException("Le périmètre et l'unité sont obligatoires."); if (r.price() == null || r.price().compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Le prix doit être positif ou nul."); }
    private void validateSpaceDeposit(AdditionalServiceRequest request, Long excludedId) {
        ServiceScope scope = scopes.findById(request.scopeId()).orElseThrow(() -> missing("Périmètre", request.scopeId()));
        if (!"SPACE".equalsIgnoreCase(scope.getCode()) || !Boolean.TRUE.equals(request.deposit())
            || Boolean.FALSE.equals(request.active())) return;
        if (request.price().compareTo(new BigDecimal("100000")) != 0)
            throw new ValidationException("La caution d'espace doit rester fixée à 100 000 Ar.");
        boolean anotherActiveDeposit = additionalServices.findActiveSpaceDeposits().stream()
                .anyMatch(existing -> !existing.getIdService().equals(excludedId));
        if (anotherActiveDeposit)
            throw new ValidationException("Un seul service de caution actif est autorisé pour les espaces.");
    }
    private void validateService(ServiceRequest r) { if (r == null) throw new ValidationException("Les données du service sont obligatoires."); clean(r.code(), "du code"); clean(r.name(), "du service"); }
    private void apply(MeetingRoomService s, ServiceRequest r) { s.setCode(r.code().trim().toUpperCase()); s.setName(r.name().trim()); s.setDescription(r.description()); }
    private void apply(PoolService s, ServiceRequest r) { s.setCode(r.code().trim().toUpperCase()); s.setName(r.name().trim()); s.setDescription(r.description()); }
    private String uniqueName(String value, boolean exists) { String v = clean(value, ""); if (exists) duplicate(); return v; }
    private String uniqueCode(String value, boolean exists) { String v = clean(value, "").toUpperCase(); if (exists) duplicate(); return v; }
    private String clean(String value, String label) { if (value == null || value.isBlank()) throw new ValidationException("Le nom " + label + " est obligatoire."); return value.trim(); }
    private void duplicate() { throw new DuplicateResourceException("Cette valeur existe déjà."); }
    private ResourceNotFoundException missing(String type, Long id) { return new ResourceNotFoundException(type + " introuvable avec l'id " + id); }
}
