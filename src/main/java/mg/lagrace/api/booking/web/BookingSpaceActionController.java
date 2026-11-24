package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;

import mg.lagrace.api.booking.dto.room.BookingResponse;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingRoomCancelRequest;
import mg.lagrace.api.booking.dto.BookingSpaceUpdateRequest;
import mg.lagrace.api.booking.dto.BookingStatusActionRequest;
import mg.lagrace.api.booking.application.BookingCancellationService;
import mg.lagrace.api.booking.application.BookingSpaceActionService;
import mg.lagrace.api.booking.application.BookingSpaceService;
import mg.lagrace.api.auth.application.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/booking-spaces")
public class BookingSpaceActionController {
    private final BookingSpaceActionService actions;
    private final BookingCancellationService cancellations;
    private final BookingSpaceService spaces;

    public BookingSpaceActionController(BookingSpaceActionService actions, BookingCancellationService cancellations,
            BookingSpaceService spaces) {
        this.actions = actions;
        this.cancellations = cancellations;
        this.spaces = spaces;
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<mg.lagrace.api.booking.dto.room.BookingResponse>> update(@PathVariable Long id,
            @Valid @RequestBody BookingSpaceUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(spaces.updateSpace(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.start(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.end(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CANCEL"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelSpace(id, request, authentication)));
    }
}
