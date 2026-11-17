package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingRoomCancelRequest;
import mg.lagrace.api.dto.booking.BookingSpaceUpdateRequest;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.services.BookingCancellationService;
import mg.lagrace.api.services.BookingSpaceActionService;
import mg.lagrace.api.services.BookingSpaceService;
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
    public ResponseEntity<ApiResponse<mg.lagrace.api.dto.booking.room.BookingResponse>> update(@PathVariable Long id,
            @Valid @RequestBody BookingSpaceUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(spaces.updateSpace(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.start(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.end(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelSpace(id, request, authentication)));
    }
}