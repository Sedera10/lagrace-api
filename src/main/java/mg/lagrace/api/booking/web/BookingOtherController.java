package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.booking.application.BookingOtherService;
import mg.lagrace.api.auth.application.RequiresPermissions;

@RestController
@RequestMapping("/booking-others")
public class BookingOtherController {
    private final BookingOtherService service;

    public BookingOtherController(BookingOtherService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<List<BookingOther>>> getAll(
            @RequestParam(required = false) Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(bookingId)));
    }

    @GetMapping("/booking/{bookingId}")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<List<BookingOther>>> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByBookingId(bookingId)));
    }

    @GetMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ"})
    public ResponseEntity<ApiResponse<BookingOther>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

}
