package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;

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
import mg.lagrace.api.booking.dto.room.BookingRoomUpdateRequest;
import mg.lagrace.api.booking.dto.room.BookingRoomExtendRequest;
import mg.lagrace.api.booking.dto.room.BookingRoomCheckInRequest;
import mg.lagrace.api.booking.dto.room.BookingRoomCheckOutRequest;
import mg.lagrace.api.booking.dto.BookingRoomCancelRequest;
import mg.lagrace.api.booking.application.BookingCancellationService;
import mg.lagrace.api.booking.application.BookingRoomService;
import mg.lagrace.api.auth.application.RequiresPermissions;

@RestController
@RequestMapping("/booking-rooms")
public class BookingRoomActionController {
    private final BookingRoomService service;
    private final BookingCancellationService cancellationService;

    public BookingRoomActionController(BookingRoomService service, BookingCancellationService cancellationService) {
        this.service = service;
        this.cancellationService = cancellationService;
    }

    @PatchMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ", "ROOM_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BookingRoomUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.updateRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/extend")
    @RequiresPermissions({"BOOKING_READ", "ROOM_EXTEND"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> extend(
            @PathVariable Long id, @Valid @RequestBody BookingRoomExtendRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.extendRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/check-in")
    @RequiresPermissions({"BOOKING_READ", "ROOM_CHECK_IN"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> checkIn(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCheckInRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.checkIn(id, request, authentication)));
    }

    @PostMapping("/{id}/check-out")
    @RequiresPermissions({"BOOKING_READ", "ROOM_CHECK_OUT"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> checkOut(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCheckOutRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.checkOut(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermissions({"BOOKING_READ", "ROOM_CANCEL"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCancelRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellationService.cancelRoom(id, request, authentication)));
    }
}
