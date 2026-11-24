package mg.lagrace.api.auth.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import mg.lagrace.api.auth.application.AccountService;
import mg.lagrace.api.auth.dto.user.AccountProfileRequest;
import mg.lagrace.api.auth.dto.user.AccountResponse;
import mg.lagrace.api.auth.dto.user.ChangePasswordRequest;
import mg.lagrace.api.shared.api.ApiResponse;

@RestController
@RequestMapping("/account/me")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AccountResponse>> getCurrentAccount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getCurrentAccount(authentication.getName()), "Compte chargé."));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AccountResponse>> updateProfile(
            Authentication authentication, @Valid @RequestBody AccountProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateProfile(authentication.getName(), request), "Profil mis à jour."));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mot de passe modifié."));
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<AccountResponse>> uploadProfilePhoto(
            Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                service.uploadProfilePhoto(authentication.getName(), file), "Photo de profil mise à jour."));
    }
}
