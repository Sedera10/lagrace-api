package mg.lagrace.api.auth.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.auth.application.RolePermissionService;
import mg.lagrace.api.auth.application.RolePermissionService.Module;
import mg.lagrace.api.auth.application.RolePermissionService.RolePermissions;
import mg.lagrace.api.auth.application.RolePermissionService.RoleSummary;
import mg.lagrace.api.auth.application.RolePermissionService.SavePermissions;
import mg.lagrace.api.shared.api.ApiResponse;

@RestController
@RequestMapping("/access-management")
public class RolePermissionController {
    private final RolePermissionService service;
    public RolePermissionController(RolePermissionService service) { this.service = service; }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleSummary>>> roles(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.roles(authentication.getName())));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<Module>>> catalog(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.catalog(authentication.getName())));
    }

    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<RolePermissions>> role(@PathVariable long roleId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.getRole(roleId, authentication.getName())));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public ResponseEntity<ApiResponse<RolePermissions>> save(@PathVariable long roleId,
            @RequestBody SavePermissions request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.save(roleId, authentication.getName(), request)));
    }
}
