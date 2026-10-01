package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.pool.PoolRateRequest;
import mg.lagrace.api.models.views.PoolRateDetail;
import mg.lagrace.api.services.PoolManagmentService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pools")
public class PoolController {
    private final PoolManagmentService poolService;

    public PoolController(PoolManagmentService poolService) {
        this.poolService = poolService;
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<PoolRateDetail>>> getRates() {
        List<PoolRateDetail> rates = poolService.allRates( 1L);
        return ResponseEntity.ok(ApiResponse.success(rates, "Liste des tarifs pour Piscine"));
    }

    @GetMapping("/rates/currents")
    public ResponseEntity<ApiResponse<List<PoolRateDetail>>> getCurrentRates() {
        List<PoolRateDetail> rates = poolService.currentRates( 1L, null);
        return ResponseEntity.ok(ApiResponse.success(rates, "Les tarifs appliqués de "));
    }

    @GetMapping("/rates/others")
    public ResponseEntity<ApiResponse<List<PoolRateDetail>>> getOtherRates() {
        List<PoolRateDetail> rates = poolService.otherRates( 1L, null);
        return ResponseEntity.ok(ApiResponse.success(rates, "Les tarifs hors actuellement appliqués"));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<PoolRateDetail>> createRate(@RequestBody PoolRateRequest request) {
        PoolRateDetail rate = poolService.SaveOrUpdatePoolRate(request);
        return ResponseEntity.status(201).body(ApiResponse.success(rate, "Tarif piscine créé"));
    }

    @PutMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<PoolRateDetail>> updateRate(
            @PathVariable Long idRate,
            @RequestBody PoolRateRequest request) {
        PoolRateDetail rate = poolService.updatePoolRate(idRate, request);
        return ResponseEntity.ok(ApiResponse.success(rate, "Tarif piscine modifié"));
    }

    @DeleteMapping("/rates/{idRate}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long idRate) {
        poolService.deletePoolRate(idRate);
        return ResponseEntity.ok(ApiResponse.success(null, "Tarif piscine supprimé"));
    }

}