package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.space.SpaceRateRequest;
import mg.lagrace.api.dto.space.SpaceRateResponse;
import mg.lagrace.api.dto.space.SpaceRequest;
import mg.lagrace.api.dto.photo.PhotoResponse;
import mg.lagrace.api.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.dto.user.UserResponse;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.services.SpaceService;
import mg.lagrace.api.services.UserService;
import mg.lagrace.api.services.PhotoService;
import mg.lagrace.api.models.enums.PhotoItemType;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/spaces")
public class SpaceController {
    private final SpaceService spaceService;
    private final PhotoService photoService;

    public SpaceController(SpaceService spaceService, PhotoService photoService) {
        this.spaceService = spaceService;
        this.photoService = photoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Space>>> Listes() {
        List<Space> spaces = spaceService.getSpaces();
        return ResponseEntity.ok(ApiResponse.success(spaces, "Liste de tous les Salle Espaces"));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<Space>> addSpace(@RequestBody SpaceRequest request) {
        Space space = spaceService.newSpace(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(space, "Nouvelle espace enregistrée."));
    }

    @PutMapping("/{idSpace}")
    public ResponseEntity<ApiResponse<Space>> updateSpace(
            @PathVariable Long idSpace, @RequestBody SpaceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                spaceService.updateSpace(idSpace, request), "Espace modifié avec succès."));
    }

    @DeleteMapping("/{idSpace}")
    public ResponseEntity<ApiResponse<Void>> deleteSpace(@PathVariable Long idSpace) {
        spaceService.deleteSpace(idSpace);
        return ResponseEntity.ok(ApiResponse.success(null, "Espace supprimé avec succès."));
    }

    // ============ TARIFS ============ //
    @GetMapping("/{idSpace}/rates")
    public ResponseEntity<ApiResponse<List<SpaceRateResponse>>> ListeTarifs(@PathVariable Long idSpace) {
        List<SpaceRateResponse> rates = spaceService.getTarifs(idSpace);
        return ResponseEntity.ok(ApiResponse.success(rates, "Les tarifs de l'espace Espaces"));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<SpaceRateResponse>> addTarif(
            @RequestBody SpaceRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                spaceService.addTarif(request), "Tarif enregistré avec succès."));
    }

    @PutMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<SpaceRateResponse>> updateTarif(
            @PathVariable Long idRate, @RequestBody SpaceRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                spaceService.updateTarif(idRate, request), "Tarif modifié avec succès."));
    }

    @DeleteMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<Void>> deleteTarif(@PathVariable Long idRate) {
        spaceService.deleteTarif(idRate);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif supprimé avec succès."));
    }

    // ============ PHOTOS ============ //
    @GetMapping("/{idSpace}/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getSpacePhotos(
            @PathVariable Long idSpace) {
        return ResponseEntity.ok(ApiResponse.success(
                photoService.listByItem(PhotoItemType.SPACE, idSpace),
                "Photos de l'espace récupérées avec succès."));
    }

    @PostMapping(value = "/{idSpace}/photos", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PhotoResponse>> uploadSpacePhoto(
            @PathVariable Long idSpace,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "false") boolean isCover) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                photoService.upload(PhotoItemType.SPACE, idSpace, file, caption, altText, isCover),
                "Photo d'espace ajoutée avec succès."));
    }

    @PutMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> updateSpacePhoto(
            @PathVariable Long photoId, @RequestBody UpdatePhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                photoService.update(photoId, request), "Photo d'espace modifiée avec succès."));
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpacePhoto(
            @PathVariable Long photoId) throws IOException {
        photoService.delete(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo d'espace supprimée avec succès."));
    }

    @PatchMapping("/{idSpace}/photos/{photoId}/cover")
    public ResponseEntity<ApiResponse<Void>> setSpaceCoverPhoto(
            @PathVariable Long idSpace, @PathVariable Long photoId) {
        photoService.assertBelongsTo(photoId, PhotoItemType.SPACE, idSpace);
        photoService.setCover(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo définie comme couverture."));
    }


    //  ================= SERVICES ADDITIONNELS ============== //
    @GetMapping("/services") 
    public ResponseEntity<ApiResponse<List<AdditionalService>>> additional() { 
        return ResponseEntity.ok(ApiResponse.success(spaceService.getAdditionalServices())); 
    }

    
}
