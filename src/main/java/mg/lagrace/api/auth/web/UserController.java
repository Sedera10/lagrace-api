package mg.lagrace.api.auth.web;

import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.auth.dto.user.UserResponse;
import mg.lagrace.api.auth.application.UserService;

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
