package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingRoomCancelRequest;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.dto.booking.pool.BookingPoolExtendRequest;
import mg.lagrace.api.dto.booking.pool.BookingPoolUpdateRequest;
import mg.lagrace.api.services.BookingCancellationService;
import mg.lagrace.api.services.BookingPoolService;
import mg.lagrace.api.services.BookingTypedLineActionService;
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
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @RequestBody BookingPoolUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(pools.update(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.startPool(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.endPool(id, request, authentication)));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> extend(@PathVariable Long id,
            @Valid @RequestBody BookingPoolExtendRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(pools.extend(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelPool(id, request, authentication)));
    }
}
