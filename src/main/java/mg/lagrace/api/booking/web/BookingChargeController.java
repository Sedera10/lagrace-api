package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingChargeUpdateRequest;
import mg.lagrace.api.booking.dto.BookingChargeVoidRequest;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.application.BookingChargeService;
import mg.lagrace.api.auth.application.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/charges")
public class BookingChargeController {
    private final BookingChargeService service;
    public BookingChargeController(BookingChargeService service) { this.service = service; }

    @PatchMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ", "CHARGE_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @Valid @RequestBody BookingChargeUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request, authentication)));
    }

    @PostMapping("/{id}/void")
    @RequiresPermissions({"BOOKING_READ", "CHARGE_VOID"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> voidCharge(@PathVariable Long id,
            @Valid @RequestBody BookingChargeVoidRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.voidCharge(id, request, authentication)));
    }
}
