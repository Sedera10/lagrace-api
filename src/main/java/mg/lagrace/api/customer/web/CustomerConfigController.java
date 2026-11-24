package mg.lagrace.api.customer.web;

import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.employee.Document;

import mg.lagrace.api.customer.IdentificationDocument;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.customer.dto.IdentificationDocumentRequest;
import mg.lagrace.api.customer.dto.NationalityRequest;
import mg.lagrace.api.customer.dto.SexRequest;
import mg.lagrace.api.reference.Nationality;
import mg.lagrace.api.reference.Sex;
import mg.lagrace.api.customer.application.CustomerConfigService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-configs")
public class CustomerConfigController {
    private final CustomerConfigService service;

    public CustomerConfigController(CustomerConfigService customerConfigService) {
        this.service = customerConfigService;
    }

    // =======================================
    // NATIONALITY
    // =======================================
    @GetMapping("/nationalities")
    public ResponseEntity<ApiResponse<List<Nationality>>> getNationalities() {
        return ResponseEntity.ok(ApiResponse.success(service.getNationalities()));
    }

    @PostMapping("/nationalities")
    public ResponseEntity<ApiResponse<Nationality>> addNationality(@RequestBody NationalityRequest request) {
        Nationality created = service.addNationality(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Nouvelle nationalité créé."));
    }

    @PutMapping("/nationalities/{id}")
    public ResponseEntity<ApiResponse<Nationality>> updateNationality(
            @PathVariable Long id,
            @RequestBody NationalityRequest request) {
        Nationality updated = service.updateNationality(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Nationalité mise à jour avec succès."));
    }

    @DeleteMapping("/nationalities/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNationality(@PathVariable Long id) {
        service.deleteNationality(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Nationalité supprimée avec succès."));
    }

    // =======================================
    // SEXE
    // =======================================
    @GetMapping("/sexes")
    public ResponseEntity<ApiResponse<List<Sex>>> getSexes() {
        return ResponseEntity.ok(ApiResponse.success(service.getSexes()));
    }

    @PostMapping("/sexes")
    public ResponseEntity<ApiResponse<Sex>> addSex(@RequestBody SexRequest request) {
        Sex created = service.addSex(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Nouveau sexe créé."));
    }

    @PutMapping("/sexes/{id}")
    public ResponseEntity<ApiResponse<Sex>> updateSex(
            @PathVariable Long id,
            @RequestBody SexRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateSex(id, request), "Sexe mis à jour avec succès."));
    }

    @DeleteMapping("/sexes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSex(@PathVariable Long id) {
        service.deleteSex(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Sexe supprimé avec succès."));
    }

    // =======================================
    // DOCUMENT D'IDENTIFICATION
    // =======================================
    @GetMapping("/ident-docs")
    public ResponseEntity<ApiResponse<List<IdentificationDocument>>> getIdentificationDocuments() {
        return ResponseEntity.ok(ApiResponse.success(service.getIdentificationDocuments()));
    }

    @PostMapping("/ident-docs")
    public ResponseEntity<ApiResponse<IdentificationDocument>> addIdentificationDocument(
            @RequestBody IdentificationDocumentRequest request) {
        IdentificationDocument created = service.addIdentificationDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Nouveau document d'identification créé."));
    }

    @PutMapping("/ident-docs/{id}")
    public ResponseEntity<ApiResponse<IdentificationDocument>> updateIdentificationDocument(
            @PathVariable Long id,
            @RequestBody IdentificationDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateIdentificationDocument(id, request),
                "Document d'identification mis à jour avec succès."));
    }

    @DeleteMapping("/ident-docs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIdentificationDocument(@PathVariable Long id) {
        service.deleteIdentificationDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document d'identification supprimé avec succès."));
    }
}
