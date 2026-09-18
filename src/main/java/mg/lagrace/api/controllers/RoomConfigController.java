package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.room.EquipmentRequest;
import mg.lagrace.api.dto.room.RoomCategoryRequest;
import mg.lagrace.api.dto.room.RoomStatusRequest;
import mg.lagrace.api.dto.room.RoomTypeRequest;
import mg.lagrace.api.dto.room.RoomTypeResponse;
import mg.lagrace.api.models.Equipment;
import mg.lagrace.api.models.RoomCategory;
import mg.lagrace.api.models.RoomStatus;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.services.RoomConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room-configs")
public class RoomConfigController {

    private final RoomConfigService service;

    public RoomConfigController(RoomConfigService service) {
        this.service = service;
    }

    // =======================================
    // STATUTS
    // =======================================
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<RoomStatus>>> getStatuses() {
        List<RoomStatus> statuses = service.getRoomStatus();
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @PostMapping("/statuses")
    public ResponseEntity<ApiResponse<RoomStatus>> addStatus(@RequestBody RoomStatusRequest request) {
        RoomStatus created = service.addStatus(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Statut créé avec succès."));
    }

    @PutMapping("/statuses/{id}")
    public ResponseEntity<ApiResponse<RoomStatus>> updateStatus(
            @PathVariable Long id,
            @RequestBody RoomStatusRequest request) {
        RoomStatus updated = service.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Statut mis à jour avec succès."));
    }

    @DeleteMapping("/statuses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long id) {
        service.deleteStatus(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Statut supprimé avec succès."));
    }

    // =======================================
    // CATÉGORIES
    // =======================================
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<RoomCategory>>> getCategories() {
        List<RoomCategory> categories = service.getRoomCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<RoomCategory>> addCategory(@RequestBody RoomCategoryRequest request) {
        RoomCategory created = service.addCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Catégorie créée avec succès."));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<RoomCategory>> updateCategory(
            @PathVariable Long id,
            @RequestBody RoomCategoryRequest request) {
        RoomCategory updated = service.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Catégorie mise à jour avec succès."));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Catégorie supprimée avec succès."));
    }

    // =======================================
    // TYPES
    // =======================================
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getTypes() {
        List<RoomType> types = service.getRoomTypes();
        return ResponseEntity.ok(ApiResponse.success(types.stream().map(this::toRoomTypeResponse).toList()));
    }

    @PostMapping("/types")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> addType(@RequestBody RoomTypeRequest request) {
        RoomType created = service.addType(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(toRoomTypeResponse(created), "Type créé avec succès."));
    }

    @PutMapping("/types/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateType(
            @PathVariable Long id,
            @RequestBody RoomTypeRequest request) {
        RoomType updated = service.updateType(id, request);
        return ResponseEntity.ok(ApiResponse.success(toRoomTypeResponse(updated), "Type mis à jour avec succès."));
    }

    @DeleteMapping("/types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id) {
        service.deleteType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Type supprimé avec succès."));
    }

    private RoomTypeResponse toRoomTypeResponse(RoomType type) {
        return new RoomTypeResponse(
                type.getIdType(),
                type.getCategory().getIdCategory(),
                type.getCategory().getName(),
                type.getName(),
                type.getDescription(),
                type.getIsAirConditioned(),
                type.getDefaultCapacity(),
                type.getCreatedAt(),
                type.getUpdatedAt());
    }

    // =======================================
    // ÉQUIPEMENTS
    // =======================================
    @GetMapping("/equipments")
    public ResponseEntity<ApiResponse<List<Equipment>>> getEquipments() {
        List<Equipment> equipments = service.getEquipments();
        return ResponseEntity.ok(ApiResponse.success(equipments));
    }

    @PostMapping("/equipments")
    public ResponseEntity<ApiResponse<Equipment>> addEquipment(@RequestBody EquipmentRequest request) {
        Equipment created = service.addEquipment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Équipement créé avec succès."));
    }

    @PutMapping("/equipments/{id}")
    public ResponseEntity<ApiResponse<Equipment>> updateEquipment(
            @PathVariable Long id,
            @RequestBody EquipmentRequest request) {
        Equipment updated = service.updateEquipment(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Équipement mis à jour avec succès."));
    }

    @DeleteMapping("/equipments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEquipment(@PathVariable Long id) {
        service.deleteEquipment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Équipement supprimé avec succès."));
    }

}
