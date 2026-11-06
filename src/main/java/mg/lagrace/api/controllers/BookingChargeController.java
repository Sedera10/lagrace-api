package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.BookingChargeUpdateRequest;
import mg.lagrace.api.dto.booking.BookingChargeVoidRequest;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.services.BookingChargeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/charges")
public class BookingChargeController {
    private final BookingChargeService service;
    public BookingChargeController(BookingChargeService service) { this.service = service; }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @Valid @RequestBody BookingChargeUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request, authentication)));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> voidCharge(@PathVariable Long id,
            @Valid @RequestBody BookingChargeVoidRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.voidCharge(id, request, authentication)));
    }
}
