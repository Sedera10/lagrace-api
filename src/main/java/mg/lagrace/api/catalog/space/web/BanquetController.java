package mg.lagrace.api.catalog.space.web;

import mg.lagrace.api.catalog.space.Space;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.space.BanquetMenuType;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.catalog.space.dto.banquet.BanquetRateRequest;
import mg.lagrace.api.catalog.space.dto.banquet.BanquetRateResponse;
import mg.lagrace.api.catalog.space.application.BanquetService;

@RestController
@RequestMapping("/banquets")
public class BanquetController {
    private final BanquetService banquetService;

    public BanquetController(BanquetService banquetService) {
        this.banquetService = banquetService;
    }

    @GetMapping("/menu-types")
    public ResponseEntity<ApiResponse<List<BanquetMenuType>>> getMenuTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                banquetService.getMenuTypes(), "Types de menu banquet récupérés avec succès."));
    }

    @GetMapping("/rates/current")
    public ResponseEntity<ApiResponse<List<BanquetRateResponse>>> getCurrentRates(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                banquetService.getCurrentRates(date), "Tarifs banquet actuels récupérés avec succès."));
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<BanquetRateResponse>>> getRates() {
        return ResponseEntity.ok(ApiResponse.success(
                banquetService.getRates(), "Tarifs banquet récupérés avec succès."));
    }

    @GetMapping("/rates/others")
    public ResponseEntity<ApiResponse<List<BanquetRateResponse>>> getOtherRates(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                banquetService.getOtherRates(date), "Autres tarifs banquet récupérés avec succès."));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<BanquetRateResponse>> addRate(
            @RequestBody BanquetRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                banquetService.addRate(request), "Tarif banquet ajouté avec succès."));
    }

    @PutMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<BanquetRateResponse>> updateRate(
            @PathVariable Long idRate, @RequestBody BanquetRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                banquetService.updateRate(idRate, request), "Tarif banquet modifié avec succès."));
    }

    @DeleteMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long idRate) {
        banquetService.deleteRate(idRate);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif banquet supprimé avec succès."));
    }

    @GetMapping("/services") 
    public ResponseEntity<ApiResponse<List<AdditionalService>>> additional() { 
        return ResponseEntity.ok(ApiResponse.success(banquetService.getAdditionalServices())); 
    }
}