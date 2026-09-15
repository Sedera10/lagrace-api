package mg.lagrace.api.controllers;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.jwt.JwtUtil;
import mg.lagrace.api.services.AuthService;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.LoginRequest;
import mg.lagrace.api.dto.LoginResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,JwtUtil jwtUtil, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            String token = jwtUtil.generateToken(authentication.getName());
                var user = authService.getUserResponse(authentication.getName());

                return ResponseEntity.ok(ApiResponse.success(
                    new LoginResponse(token, user), "Authentification réussie"));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error("AUTHENTICATION_FAILED",
                            "Nom d'utilisateur ou mot de passe incorrect"));
        }
    }

    @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("TOKEN_MISSING","Le token Bearer est obligatoire"));
        }

        String token = authorization.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(
                            "TOKEN_INVALID",
                            "Le token est invalide ou expiré"));
        }

        authService.revoke(
                jwtUtil.extractTokenId(token),
                jwtUtil.extractExpiration(token));

        return ResponseEntity.ok(
                ApiResponse.success(null, "Déconnexion réussie"));
    }
}
