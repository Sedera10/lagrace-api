package mg.lagrace.api.controllers;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.menu.MenuItemResponse;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.services.MenuItemService;
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

    @GetMapping("/{id}/rate")
    public MenuItemRate getMenuItemRate(
            @PathVariable("id") Long menuItemId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return menuItemService.getApplicableRate(menuItemId, date);
    }
}
