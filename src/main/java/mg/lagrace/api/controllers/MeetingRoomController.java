package mg.lagrace.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.meeting.MeetingRoomRateRequest;
import mg.lagrace.api.dto.meeting.MeetingRoomRateResponse;
import mg.lagrace.api.dto.meeting.MeetingRoomRequest;
import mg.lagrace.api.models.MeetingRoom;
import mg.lagrace.api.services.MeetingRoomManagementService;

@RestController
@RequestMapping("/meeting-rooms")
public class MeetingRoomController {
    private final MeetingRoomManagementService service;

    public MeetingRoomController(MeetingRoomManagementService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingRoom>>> listRooms() { 
        return ResponseEntity.ok(ApiResponse.success(service.listRooms())); 
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
    public ResponseEntity<ApiResponse<List<MeetingRoomRateResponse>>> listRates() { return ResponseEntity.ok(ApiResponse.success(service.listRates())); }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<MeetingRoomRateResponse>> createRate(@RequestBody MeetingRoomRateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.createRate(request))); }

    @PutMapping("/rates/{id}")
    public ResponseEntity<ApiResponse<MeetingRoomRateResponse>> updateRate(@PathVariable Long id, @RequestBody MeetingRoomRateRequest request) { return ResponseEntity.ok(ApiResponse.success(service.updateRate(id, request))); }

    @DeleteMapping("/rates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long id) { service.deleteRate(id); return ResponseEntity.ok(ApiResponse.success(null)); }
}
