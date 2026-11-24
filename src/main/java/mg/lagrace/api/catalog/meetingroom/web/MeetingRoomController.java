package mg.lagrace.api.catalog.meetingroom.web;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.catalog.meetingroom.dto.meeting.MeetingRoomRateRequest;
import mg.lagrace.api.catalog.meetingroom.dto.meeting.MeetingRoomRateResponse;
import mg.lagrace.api.catalog.meetingroom.dto.meeting.MeetingRoomRequest;
import mg.lagrace.api.catalog.meetingroom.application.MeetingRoomManagementService;

@RestController
@RequestMapping("/meeting-rooms")
public class MeetingRoomController {
    private final MeetingRoomManagementService service;

    public MeetingRoomController(MeetingRoomManagementService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingRoom>>> listRooms(
            @RequestParam(required = false, defaultValue = "true") boolean active) {
        return ResponseEntity.ok(ApiResponse.success(service.listRooms())); 
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<MeetingRoom>>> availability(@RequestParam LocalDate date,
            @RequestParam(required = false) Long excludeBookingMeetingRoomId) {
        return ResponseEntity.ok(ApiResponse.success(service.availableRooms(date, excludeBookingMeetingRoomId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingRoom>> createRoom(
        @RequestBody MeetingRoomRequest request) { 
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createRoom(request))); 
        }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MeetingRoom>> updateRoom(@PathVariable Long id, @RequestBody MeetingRoomRequest request) { return ResponseEntity.ok(ApiResponse.success(service.updateRoom(id, request))); }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) { service.deleteRoom(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<MeetingRoomRateResponse>>> listRates(
            @RequestParam(required = false) Long serviceId, @RequestParam(required = false) LocalDate date) {
        if ((serviceId == null) != (date == null)) {
            throw new mg.lagrace.api.exceptions.ValidationException("La formule et la date doivent être renseignées ensemble.");
        }
        return ResponseEntity.ok(ApiResponse.success(date == null
                ? service.listRates() : service.currentRates(serviceId, date)));
    }

    @GetMapping("/rates/current")
    public ResponseEntity<ApiResponse<List<MeetingRoomRateResponse>>> currentRates(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(service.currentRates(date)));
    }

    @GetMapping("/rates/others")
    public ResponseEntity<ApiResponse<List<MeetingRoomRateResponse>>> otherRates(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(service.otherRates(date)));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<MeetingRoomRateResponse>> createRate(@RequestBody MeetingRoomRateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.createRate(request))); }

    @PutMapping("/rates/{id}")
    public ResponseEntity<ApiResponse<MeetingRoomRateResponse>> updateRate(@PathVariable Long id, @RequestBody MeetingRoomRateRequest request) { return ResponseEntity.ok(ApiResponse.success(service.updateRate(id, request))); }

    @DeleteMapping("/rates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long id) { service.deleteRate(id); return ResponseEntity.ok(ApiResponse.success(null)); }
}
