package mg.lagrace.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingOtherUpdateRequest;
import mg.lagrace.api.dto.booking.BookingOtherVoidRequest;
import mg.lagrace.api.services.BookingOtherService;

@RestController
@RequestMapping("/others")
public class BookingOtherActionController {
    private final BookingOtherService service;

    public BookingOtherActionController(BookingOtherService service) {
        this.service = service;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BookingOtherUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.updateInBooking(id, request, username(authentication))));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> voidOther(
            @PathVariable Long id, @Valid @RequestBody BookingOtherVoidRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.voidInBooking(id, request, username(authentication))));
    }

    private String username(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
