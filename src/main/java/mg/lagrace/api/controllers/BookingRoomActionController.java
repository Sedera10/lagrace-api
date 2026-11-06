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
import mg.lagrace.api.dto.booking.room.BookingRoomUpdateRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomExtendRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomCheckInRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomCheckOutRequest;
import mg.lagrace.api.dto.booking.BookingRoomCancelRequest;
import mg.lagrace.api.services.BookingCancellationService;
import mg.lagrace.api.services.BookingRoomService;

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
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BookingRoomUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.updateRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> extend(
            @PathVariable Long id, @Valid @RequestBody BookingRoomExtendRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.extendRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> checkIn(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCheckInRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.checkIn(id, request, authentication)));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> checkOut(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCheckOutRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.checkOut(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(
            @PathVariable Long id, @Valid @RequestBody BookingRoomCancelRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellationService.cancelRoom(id, request, authentication)));
    }
}