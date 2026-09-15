package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.UserResponse;
import mg.lagrace.api.services.UserService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> Teste() {
        List<UserResponse> users = userService.getUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Liste des Utilisateurs"));
    }
}
