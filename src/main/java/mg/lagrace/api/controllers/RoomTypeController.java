package mg.lagrace.api.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.photo.PhotoResponse;
import mg.lagrace.api.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.dto.room.equipment.AddRoomTypeEquipmentRequest;
import mg.lagrace.api.dto.room.equipment.RoomTypeEquipmentResponse;
import mg.lagrace.api.dto.room.equipment.UpdateRoomTypeEquipmentRequest;
import mg.lagrace.api.dto.room.photo.RoomPhotoResponse;
import mg.lagrace.api.dto.room.photo.UpdateRoomPhotoRequest;
import mg.lagrace.api.dto.room.rate.CreateRoomRateRequest;
import mg.lagrace.api.dto.room.rate.RoomRateResponse;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRow;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRowRequest;
import mg.lagrace.api.dto.room.rate.UpdateRoomRateRequest;
import mg.lagrace.api.services.PhotoService;
import mg.lagrace.api.services.RoomTypeService;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.RoomTypeEquipment;
import mg.lagrace.api.models.enums.PhotoItemType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;




@RestController
@RequestMapping("/room-types")
public class RoomTypeController {
    private final RoomTypeService roomTypeService;
    private final PhotoService photoService;

    public RoomTypeController(RoomTypeService roomTypeService, PhotoService photoService) {
        this.roomTypeService = roomTypeService;
        this.photoService = photoService;
    }

    @GetMapping("/{typeId}/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getTypePhotos(
            @PathVariable Long typeId
    ) {

        List<PhotoResponse> photos = photoService.listByItem(PhotoItemType.ROOM_TYPE,typeId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        photos,
                        "Photos du type de chambre récupérées avec succès."
                )
        );
    }

    @PostMapping(value = "/{typeId}/photos",consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PhotoResponse>> uploadRoomTypePhoto(
            @PathVariable Long typeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "false") boolean isCover
    ) throws IOException {

        PhotoResponse saved = photoService.upload(
               PhotoItemType.ROOM_TYPE ,typeId, file, caption, altText, isCover
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved, "Photo de chambre ajoutée avec succès."));
    }
    
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long photoId) throws IOException {
        photoService.delete(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo supprimée avec succès."));
    }

    @PutMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> updatePhotoCaption(
            @PathVariable Long photoId,
            @RequestBody UpdatePhotoRequest request
    ) {
        PhotoResponse updated = photoService.update(photoId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Légende mise à jour avec succès."
        ));
    }

    @PatchMapping("/{typeId}/photos/{photoId}/cover")
    public ResponseEntity<ApiResponse<Void>> setCoverPhoto(
            @PathVariable Long typeId,
            @PathVariable Long photoId
    ) {
        photoService.assertBelongsTo(photoId, PhotoItemType.ROOM_TYPE, typeId);
        photoService.setCover(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo définie comme couverture."));
    }
    


    // ==================== EQUIPEMENTS ================== //
    @GetMapping("/{typeId}/equipments")
    public ResponseEntity<ApiResponse<List<RoomTypeEquipmentResponse>>> getEquipments(@PathVariable Long typeId) {
        List<RoomTypeEquipment> equipments = roomTypeService.getEquipmentsByType(typeId);
        List<RoomTypeEquipmentResponse> response = equipments.stream()
            .map(RoomTypeEquipmentResponse::from)
            .toList();

        return ResponseEntity.ok(ApiResponse.success(response, "Équipements du type récupérés"));
    }

    @PostMapping("/{typeId}/equipments")
    public ResponseEntity<ApiResponse<RoomTypeEquipmentResponse>> addEquipment(
            @PathVariable Long typeId,
            @RequestBody AddRoomTypeEquipmentRequest request
    ) {
        RoomTypeEquipment saved = roomTypeService.addOrUpdateEquipment(
            typeId,
            request.equipmentId(),
            request.quantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(RoomTypeEquipmentResponse.from(saved), "Équipement ajouté"));
    }

    @PutMapping("/{typeId}/equipments/{equipmentId}")
    public ResponseEntity<ApiResponse<RoomTypeEquipmentResponse>> updateEquipment(
            @PathVariable Long typeId,
            @PathVariable Long equipmentId,
            @RequestBody UpdateRoomTypeEquipmentRequest request
    ) {
        RoomTypeEquipment updated = roomTypeService.addOrUpdateEquipment(
            typeId,
            equipmentId,
            request.quantity()
        );

        return ResponseEntity.ok(ApiResponse.success(RoomTypeEquipmentResponse.from(updated), "Quantité mise à jour"));
    }

    @DeleteMapping("/{typeId}/equipments/{equipmentId}")
    public ResponseEntity<ApiResponse<Void>> removeEquipment(
            @PathVariable Long typeId,
            @PathVariable Long equipmentId
    ) {
        roomTypeService.removeEquipmentFromType(typeId, equipmentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Équipement retiré du type"));
    }

    // ============ TARIFS ===============
    @GetMapping("/{typeId}/tarifs")
    public ResponseEntity<ApiResponse<List<RoomRateResponse>>> getRates(@PathVariable Long typeId,@RequestParam(required = false) LocalDate date ) {
        List<RoomRateResponse> tarifs = roomTypeService.listTarifs(typeId, date);
        return ResponseEntity.ok(ApiResponse.success(tarifs, "Tarifs du type de chambre récupérés"));
    }

    @PostMapping("/{typeId}/tarifs")
    public ResponseEntity<ApiResponse<RoomRateResponse>> addRate(
            @PathVariable Long typeId,
            @RequestBody CreateRoomRateRequest  request
    ) {
        RoomRateResponse saved = roomTypeService.createRate(typeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved, "Tarif ajouté avec succès"));
    }

    @PutMapping("/{typeId}/tarifs/{rateId}")
    public ResponseEntity<ApiResponse<RoomRateResponse>> updateRate(
            @PathVariable Long typeId,
            @PathVariable Long rateId,
            @RequestBody UpdateRoomRateRequest request
    ) {
        RoomRateResponse updated = roomTypeService.updateRate(typeId, rateId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Tarif mis à jour avec succès"));
    }

    @DeleteMapping("/{typeId}/tarifs/{rateId}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(
            @PathVariable Long typeId,
            @PathVariable Long rateId
    ) {
        roomTypeService.deleteRate(rateId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif supprimé avec succès"));
    }
    
    
}
