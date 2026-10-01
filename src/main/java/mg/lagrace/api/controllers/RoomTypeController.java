package mg.lagrace.api.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.room.equipment.AddRoomTypeEquipmentRequest;
import mg.lagrace.api.dto.room.equipment.RoomTypeEquipmentResponse;
import mg.lagrace.api.dto.room.equipment.UpdateRoomTypeEquipmentRequest;
import mg.lagrace.api.dto.room.photo.RoomPhotoResponse;
import mg.lagrace.api.dto.room.photo.UpdateRoomPhotoRequest;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRow;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRowRequest;
import mg.lagrace.api.services.RoomTypeService;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.RoomTypeEquipment;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/{typeId}/photos")
    public ResponseEntity<ApiResponse<List<RoomPhotoResponse>>> getTypePhotos(
            @PathVariable Long typeId
    ) {

        List<RoomPhoto> photos = roomTypeService.getPhotoByType(typeId);
        List<RoomPhotoResponse> photoResponses = photos.stream()
                .map(RoomPhotoResponse::from)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        photoResponses,
                        "Photos du type de chambre récupérées avec succès."
                )
        );
    }

    @PostMapping(value = "/{typeId}/photos",consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<RoomPhotoResponse>> uploadRoomTypePhoto(
            @PathVariable Long typeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(defaultValue = "false") boolean isCover
    ) throws IOException {

        RoomPhoto saved = roomTypeService.uploadForRoomType(
                typeId, file, caption, isCover
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(RoomPhotoResponse.from(saved), "Photo de type ajoutée avec succès."));
    }
    
    @DeleteMapping("/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@PathVariable Long photoId) throws IOException {
        roomTypeService.deletePhoto(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo supprimée avec succès."));
    }

    @PutMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<RoomPhotoResponse>> updatePhotoCaption(
            @PathVariable Long photoId,
            @RequestBody UpdateRoomPhotoRequest request
    ) {
        RoomPhoto updated = roomTypeService.updatePhotoCaption(photoId, request.caption());
        return ResponseEntity.ok(ApiResponse.success(
                RoomPhotoResponse.from(updated), "Légende mise à jour avec succès."
        ));
    }

    @PatchMapping("/{typeId}/photos/{photoId}/cover")
    public ResponseEntity<ApiResponse<RoomPhotoResponse>> setCoverPhoto(
            @PathVariable Long typeId,
            @PathVariable Long photoId
    ) {
        RoomPhoto photo = roomTypeService.setCoverPhoto(typeId, photoId);
        return ResponseEntity.ok(ApiResponse.success(RoomPhotoResponse.from(photo), "Photo définie comme couverture."));
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
    public ResponseEntity<ApiResponse<List<RoomTypeRateRow>>> getRates(@PathVariable Long typeId) {
        List<RoomTypeRateRow> tarifs = roomTypeService.getRateRowsByType(typeId);
        return ResponseEntity.ok(ApiResponse.success(tarifs, "Tarifs du type récupérés"));
    }

    @PostMapping("/{typeId}/tarifs")
    public ResponseEntity<ApiResponse<RoomTypeRateRow>> addRate(
            @PathVariable Long typeId,
            @RequestBody RoomTypeRateRowRequest request
    ) {
        RoomTypeRateRow saved = roomTypeService.upsertRateRowForType(typeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved, "Tarif ajouté avec succès"));
    }

    @PutMapping("/{typeId}/tarifs")
    public ResponseEntity<ApiResponse<RoomTypeRateRow>> updateRate(
            @PathVariable Long typeId,
            @RequestBody RoomTypeRateRowRequest request
    ) {
        RoomTypeRateRow updated = roomTypeService.upsertRateRowForType(typeId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Tarif mis à jour avec succès"));
    }

    @DeleteMapping("/{typeId}/tarifs")
    public ResponseEntity<ApiResponse<Void>> deleteRate(
            @PathVariable Long typeId,
            @RequestParam("validFrom") LocalDate validFrom,
            @RequestParam(value = "validTo", required = false) LocalDate validTo
    ) {
        roomTypeService.deleteRateRowForType(typeId, validFrom, validTo);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif supprimé avec succès"));
    }
    
    
}
