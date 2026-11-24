package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingOtherCreateRequest;
import mg.lagrace.api.booking.application.BookingOtherService;
import mg.lagrace.api.auth.application.RequiresPermissions;

@RestController
@RequestMapping("/bookings")
public class BookingOtherBookingController {
    private final BookingOtherService service;

    public BookingOtherBookingController(BookingOtherService service) {
        this.service = service;
    }

    @PostMapping("/{bookingId}/others")
    @RequiresPermissions({"BOOKING_READ", "OTHER_ADD"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> add(
            @PathVariable Long bookingId, @Valid @RequestBody BookingOtherCreateRequest request,
            Authentication authentication) {
        BookingDetailResponse response = service.addToBooking(bookingId, request,
                authentication == null ? null : authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
