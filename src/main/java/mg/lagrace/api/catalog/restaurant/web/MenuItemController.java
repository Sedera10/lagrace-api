package mg.lagrace.api.catalog.restaurant.web;

import mg.lagrace.api.catalog.restaurant.MenuItemRate;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.catalog.restaurant.dto.menu.MenuItemRequest;
import mg.lagrace.api.catalog.restaurant.dto.menu.MenuItemResponse;
import mg.lagrace.api.catalog.restaurant.application.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {
    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenuItemsWithPrices(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getMenuItemsWithPrices(date)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItem(
            @PathVariable Long id,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getMenuItem(id, date)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(menuItemService.createMenuItem(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long id, @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.updateMenuItem(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/rate")
    public MenuItemRate getMenuItemRate(
            @PathVariable("id") Long menuItemId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return menuItemService.getApplicableRate(menuItemId, date);
    }
}
