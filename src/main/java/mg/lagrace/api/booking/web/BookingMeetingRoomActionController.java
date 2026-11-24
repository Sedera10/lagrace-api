package mg.lagrace.api.booking.web;

import mg.lagrace.api.booking.core.Booking;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingRoomCancelRequest;
import mg.lagrace.api.booking.dto.BookingStatusActionRequest;
import mg.lagrace.api.booking.dto.meeting.BookingMeetingRoomUpdateRequest;
import mg.lagrace.api.booking.application.BookingMeetingRoomService;
import mg.lagrace.api.booking.application.BookingCancellationService;
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
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> update(@PathVariable Long id,
            @RequestBody BookingMeetingRoomUpdateRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(meetingRooms.update(id, request, authentication)));
    }

    @PostMapping("/{id}/start")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> start(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.startMeetingRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/end")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_UPDATE"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> end(@PathVariable Long id,
            @Valid @RequestBody BookingStatusActionRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(actions.endMeetingRoom(id, request, authentication)));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermissions({"BOOKING_READ", "BOOKING_CANCEL"})
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancel(@PathVariable Long id,
            @Valid @RequestBody BookingRoomCancelRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cancellations.cancelMeetingRoom(id, request, authentication)));
    }
}
