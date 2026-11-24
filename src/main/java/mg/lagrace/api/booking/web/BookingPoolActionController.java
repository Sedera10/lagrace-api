package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.pool.Pool;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingRoomCancelRequest;
import mg.lagrace.api.booking.dto.BookingStatusActionRequest;
import mg.lagrace.api.booking.dto.pool.BookingPoolExtendRequest;
import mg.lagrace.api.booking.dto.pool.BookingPoolUpdateRequest;
import mg.lagrace.api.booking.application.BookingCancellationService;
import mg.lagrace.api.booking.application.BookingPoolService;
import mg.lagrace.api.booking.application.BookingTypedLineActionService;
import mg.lagrace.api.auth.application.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/booking-pools")
public class BookingPoolActionController {
    private final BookingTypedLineActionService actions;
    private final BookingCancellationService cancellations;
    private final BookingPoolService pools;

    public BookingPoolActionController(BookingTypedLineActionService actions, BookingCancellationService cancellations,
            BookingPoolService pools) {
        this.actions = actions;
        this.cancellations = cancellations;
        this.pools = pools;
    }

    @PatchMapping("/{id}")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @RequestBody BookingPoolUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(pools.update(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.startPool(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.endPool(id, request, authentication)));
    }

    @PostMapping("/{id}/extend")
    @RequiresPermissions({"BOOKING_READ", "ROOM_EXTEND"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> extend(@PathVariable Long id,
            @Valid @RequestBody BookingPoolExtendRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(pools.extend(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CANCEL"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelPool(id, request, authentication)));
    }
}
