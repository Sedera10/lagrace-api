package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingRoomCancelRequest;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.dto.booking.meeting.BookingMeetingRoomUpdateRequest;
import mg.lagrace.api.services.BookingMeetingRoomService;
import mg.lagrace.api.services.BookingCancellationService;
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
@RequestMapping("/booking-meeting-rooms")
public class BookingMeetingRoomActionController {
    private final BookingTypedLineActionService actions;
    private final BookingCancellationService cancellations;
    private final BookingMeetingRoomService meetingRooms;

    public BookingMeetingRoomActionController(BookingTypedLineActionService actions, BookingCancellationService cancellations, BookingMeetingRoomService meetingRooms) {
        this.actions = actions;
        this.cancellations = cancellations;
        this.meetingRooms = meetingRooms;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @RequestBody BookingMeetingRoomUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(meetingRooms.update(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.startMeetingRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.endMeetingRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelMeetingRoom(id, request, authentication)));
    }
}
