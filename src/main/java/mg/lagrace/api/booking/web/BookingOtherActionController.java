package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingOtherUpdateRequest;
import mg.lagrace.api.booking.dto.BookingOtherVoidRequest;
import mg.lagrace.api.booking.application.BookingOtherService;
import mg.lagrace.api.auth.application.RequiresPermissions;

@RestController
@RequestMapping("/others")
public class BookingOtherActionController {
    private final BookingOtherService service;

    public BookingOtherActionController(BookingOtherService service) {
        this.service = service;
    }

    @PatchMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BookingOtherUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.updateInBooking(id, request, username(authentication))));
    }

    @PostMapping("/{id}/void")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> voidOther(
            @PathVariable Long id, @Valid @RequestBody BookingOtherVoidRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.voidInBooking(id, request, username(authentication))));
    }

    private String username(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
