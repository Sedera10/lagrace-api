package mg.lagrace.api.controllers;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.system.*;
import mg.lagrace.api.dto.user.UserResponse;
import mg.lagrace.api.models.*;
import mg.lagrace.api.services.SystemConfigService;

@RestController
@RequestMapping("/system-configs")
public class SystemConfigController {
    private final SystemConfigService service;
    public SystemConfigController(SystemConfigService service) { this.service = service; }

    @GetMapping("/calendar-days") public ResponseEntity<ApiResponse<List<CalendarDay>>> days(@RequestParam(required = false) LocalDate start, @RequestParam(required = false) LocalDate end) { return ResponseEntity.ok(ApiResponse.success(service.getCalendarDays(start, end))); }
    @PutMapping("/calendar-days") public ResponseEntity<ApiResponse<CalendarDay>> day(@RequestBody CalendarDayRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateCalendarDay(r))); }

    @GetMapping("/roles") public ResponseEntity<ApiResponse<List<Role>>> roles() { return ResponseEntity.ok(ApiResponse.success(service.getRoles())); }
    @PostMapping("/roles") public ResponseEntity<ApiResponse<Role>> addRole(@RequestBody RoleRequest r) { return created(service.addRole(r)); }
    @PutMapping("/roles/{id}") public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable Long id, @RequestBody RoleRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateRole(id, r))); }
    @DeleteMapping("/roles/{id}") public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) { service.deleteRole(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/users") public ResponseEntity<ApiResponse<List<UserResponse>>> users() { return ResponseEntity.ok(ApiResponse.success(service.getUsers())); }
    @PostMapping("/users") public ResponseEntity<ApiResponse<UserResponse>> addUser(@RequestBody UserRequest r) { return created(service.addUser(r)); }
    @PutMapping("/users/{id}") public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @RequestBody UserRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateUser(id, r))); }
    @DeleteMapping("/users/{id}") public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) { service.deleteUser(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    private <T> ResponseEntity<ApiResponse<T>> created(T value) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(value)); }
}
