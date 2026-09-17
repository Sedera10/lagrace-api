package mg.lagrace.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.validation.ValidationException;
import mg.lagrace.api.dto.room.EquipmentRequest;
import mg.lagrace.api.dto.room.RoomCategoryRequest;
import mg.lagrace.api.dto.room.RoomStatusRequest;
import mg.lagrace.api.dto.room.RoomTypeRequest;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.Equipment;
import mg.lagrace.api.models.RoomCategory;
import mg.lagrace.api.models.RoomStatus;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.repositories.EquipmentRepository;
import mg.lagrace.api.repositories.RoomCategoryRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;

@Service
public class RoomConfigService {
    private final RoomStatusRepository statusRepository;
    private final RoomCategoryRepository categoryRepository;
    private final RoomTypeRepository typeRepository;
    private final EquipmentRepository equipmentRepository;

    public RoomConfigService(
            RoomStatusRepository statusRepository, RoomCategoryRepository categoryRepository,
            RoomTypeRepository typeRepository,EquipmentRepository equipmentRepository) {

        this.statusRepository = statusRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.equipmentRepository = equipmentRepository;
    }
    // =======================================
    // GESTION STATUS DE CHAMBRE
    // =======================================
    public List<RoomStatus> getRoomStatus() {
        return statusRepository.findAll();
    }
    public RoomStatus addStatus(RoomStatusRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom du statut est obligatoire.");
        }
        String normalizedName = request.name().trim();
        if (statusRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le statut '" + normalizedName + "' existe déjà.");
        }

        RoomStatus newStatus = new RoomStatus();
        newStatus.setName(normalizedName);
        newStatus.setColorCode(request.colorCode());

        return statusRepository.save(newStatus);
    }

    public RoomStatus updateStatus(Long id, RoomStatusRequest request) {
        if (id == null) {
            throw new ValidationException("L'identifiant du statut est obligatoire.");
        }
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom du statut est obligatoire.");
        }
        String normalizedName = request.name().trim();
        RoomStatus existing = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Statut introuvable avec l'id " + id));
        if (!existing.getName().equals(normalizedName)
                && statusRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le statut '" + normalizedName + "' existe déjà.");
        }
        existing.setName(normalizedName);
        existing.setColorCode(request.colorCode());

        return statusRepository.save(existing);
    }

    public void deleteStatus(Long id) {
        if (id == null) {
            throw new ValidationException("L'identifiant du statut est obligatoire.");
        }
        if (!statusRepository.existsById(id)) {
            throw new ResourceNotFoundException("Statut introuvable avec l'id " + id);
        }
        statusRepository.deleteById(id);
    }

    // =======================================
    // GESTION CATEGORIE DE CHAMBRE
    // =======================================
    public List<RoomCategory> getRoomCategories() {
        return categoryRepository.findAll();
    }

    public RoomCategory addCategory(RoomCategoryRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom de la catégorie est obligatoire.");
        }
        String normalizedName = request.name().trim();
        if (categoryRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("La catégorie '" + normalizedName + "' existe déjà.");
        }
        RoomCategory category = new RoomCategory();
        category.setName(normalizedName);
        return categoryRepository.save(category);
    }

    public RoomCategory updateCategory(Long id, RoomCategoryRequest request) {
        if (id == null) {
            throw new ValidationException("L'identifiant de la catégorie est obligatoire.");
        }
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom de la catégorie est obligatoire.");
        }
        String normalizedName = request.name().trim();
        RoomCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id " + id));
        if (!existing.getName().equals(normalizedName)
                && categoryRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("La catégorie '" + normalizedName + "' existe déjà.");
        }
        existing.setName(normalizedName);
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Long id) {
        if (id == null) {
            throw new ValidationException("L'identifiant de la catégorie est obligatoire.");
        }
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie introuvable avec l'id " + id);
        }
        categoryRepository.deleteById(id);
    }

    // =======================================
    // GESTION TYPE DE CHAMBRE
    // =======================================
    public List<RoomType> getRoomTypes() {
        return typeRepository.findAll();
    }

    public RoomType addType(RoomTypeRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom du type est obligatoire.");
        }
        if (request.categoryId() == null) {
            throw new ValidationException("L'identifiant de la catégorie est obligatoire.");
        }
        String normalizedName = request.name().trim();
        if (typeRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le type '" + normalizedName + "' existe déjà.");
        }
        RoomCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id " + request.categoryId()));

        RoomType type = new RoomType();
        type.setName(normalizedName);
        type.setDescription(request.description());
        type.setIsAirConditioned(request.isAirConditioned() != null ? request.isAirConditioned() : false);
        type.setDefaultCapacity(request.defaultCapacity());
        type.setCategory(category);
        return typeRepository.save(type);
    }

    public RoomType updateType(Long id, RoomTypeRequest request) {
        if (id == null) {
            throw new ValidationException("L'identifiant du type est obligatoire.");
        }
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom du type est obligatoire.");
        }
        if (request.categoryId() == null) {
            throw new ValidationException("L'identifiant de la catégorie est obligatoire.");
        }
        String normalizedName = request.name().trim();
        RoomType existing = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type introuvable avec l'id " + id));
        if (!existing.getName().equals(normalizedName)
                && typeRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le type '" + normalizedName + "' existe déjà.");
        }
        RoomCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'id " + request.categoryId()));

        existing.setName(normalizedName);
        existing.setDescription(request.description());
        existing.setIsAirConditioned(request.isAirConditioned() != null ? request.isAirConditioned() : false);
        existing.setDefaultCapacity(request.defaultCapacity());
        existing.setCategory(category);
        return typeRepository.save(existing);
    }

    public void deleteType(Long id) {
        if (id == null) {
            throw new ValidationException("L'identifiant du type est obligatoire.");
        }
        if (!typeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Type introuvable avec l'id " + id);
        }
        typeRepository.deleteById(id);
    }

    // =======================================
    // GESTION EQUIPEMENT DE CHAMBRE
    // =======================================
    public List<Equipment> getEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment addEquipment(EquipmentRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom de l'équipement est obligatoire.");
        }
        String normalizedName = request.name().trim();
        if (equipmentRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("L'équipement '" + normalizedName + "' existe déjà.");
        }
        Equipment equipment = new Equipment();
        equipment.setName(normalizedName);
        equipment.setDescription(request.description());
        return equipmentRepository.save(equipment);
    }

    public Equipment updateEquipment(Long id, EquipmentRequest request) {
        if (id == null) {
            throw new ValidationException("L'identifiant de l'équipement est obligatoire.");
        }
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Le nom de l'équipement est obligatoire.");
        }
        String normalizedName = request.name().trim();
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable avec l'id " + id));
        if (!existing.getName().equals(normalizedName)
                && equipmentRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("L'équipement '" + normalizedName + "' existe déjà.");
        }
        existing.setName(normalizedName);
        existing.setDescription(request.description());
        return equipmentRepository.save(existing);
    }

    public void deleteEquipment(Long id) {
        if (id == null) {
            throw new ValidationException("L'identifiant de l'équipement est obligatoire.");
        }
        if (!equipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Équipement introuvable avec l'id " + id);
        }
        equipmentRepository.deleteById(id);
    }
}
