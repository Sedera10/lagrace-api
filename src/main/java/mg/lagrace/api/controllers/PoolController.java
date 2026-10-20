package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.photo.PhotoResponse;
import mg.lagrace.api.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.dto.pool.PoolRateRequest;
import mg.lagrace.api.dto.pool.PoolRateResponse;
import mg.lagrace.api.models.Pool;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.services.PhotoService;
import mg.lagrace.api.services.PoolManagmentService;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pools")
public class PoolController {
    private final PoolManagmentService poolService;
    private final PhotoService photoService;

    public PoolController(PoolManagmentService poolService, PhotoService photoService) {
        this.poolService = poolService;
        this.photoService = photoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Pool>>> Listes() {
        List<Pool> spaces = poolService.getPools();
        return ResponseEntity.ok(ApiResponse.success(spaces, "Liste de tous les Piscines"));
    }

    // ================ TARIFS ================= //

    @GetMapping("/{idPool}/rates")
    public ResponseEntity<ApiResponse<List<PoolRateResponse>>> getRates(@PathVariable Long idPool) {
        List<PoolRateResponse> rates = poolService.allRates(idPool);
        return ResponseEntity.ok(ApiResponse.success(rates, "Liste des tarifs pour Piscine"));
    }

    @GetMapping("/{idPool}/rates/currents")
    public ResponseEntity<ApiResponse<List<PoolRateResponse>>> getCurrentRates(@PathVariable Long idPool) {
        List<PoolRateResponse> rates = poolService.currentRates(idPool, null);
        return ResponseEntity.ok(ApiResponse.success(rates, "Les tarifs appliqués de "));
    }

    @GetMapping("/{idPool}/rates/others")
    public ResponseEntity<ApiResponse<List<PoolRateResponse>>> getOtherRates(@PathVariable Long idPool) {
        List<PoolRateResponse> rates = poolService.otherRates(idPool, null);
        return ResponseEntity.ok(ApiResponse.success(rates, "Les tarifs hors actuellement appliqués"));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<PoolRateResponse>> createRate(@RequestBody PoolRateRequest request) {
        PoolRateResponse rate = poolService.SaveOrUpdatePoolRate(request);
        return ResponseEntity.status(201).body(ApiResponse.success(rate, "Tarif piscine créé"));
    }

    @PutMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<PoolRateResponse>> updateRate(
            @PathVariable Long idRate,
            @RequestBody PoolRateRequest request) {
        PoolRateResponse rate = poolService.updatePoolRate(idRate, request);
        return ResponseEntity.ok(ApiResponse.success(rate, "Tarif piscine modifié"));
    }

    @DeleteMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long idRate) {
        poolService.deletePoolRate(idRate);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif piscine supprimé"));
    }

    // ================ PHOTOS ================= //
    @GetMapping("/{idPool}/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> getSpacePhotos(
            @PathVariable Long idPool) {
        return ResponseEntity.ok(ApiResponse.success(
                photoService.listByItem(PhotoItemType.POOL, idPool),
                "Photos de la piscine récupérées avec succès."));
    }

    @PostMapping(value = "/{idPool}/photos", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PhotoResponse>> uploadSpacePhoto(
            @PathVariable Long idPool,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "false") boolean isCover) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                photoService.upload(PhotoItemType.POOL, idPool, file, caption, altText, isCover),
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

    @PatchMapping("/{idPool}/photos/{photoId}/cover")
    public ResponseEntity<ApiResponse<Void>> setSpaceCoverPhoto(
            @PathVariable Long idPool, @PathVariable Long photoId) {
        photoService.assertBelongsTo(photoId, PhotoItemType.POOL, idPool);
        photoService.setCover(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo définie comme couverture."));
    }

}