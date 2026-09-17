package mg.lagrace.api.controllers;

import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.service.*;
import mg.lagrace.api.models.*;
import mg.lagrace.api.services.ServiceConfigService;

@RestController
@RequestMapping("/service-configs")
public class ServiceConfigController {
    private final ServiceConfigService service;
    public ServiceConfigController(ServiceConfigService service) { this.service = service; }

    @GetMapping("/space-events") public ResponseEntity<ApiResponse<List<SpaceEvent>>> events() { return ResponseEntity.ok(ApiResponse.success(service.getSpaceEvents())); }
    @PostMapping("/space-events") public ResponseEntity<ApiResponse<SpaceEvent>> addEvent(@RequestBody String name) { return created(service.addSpaceEvent(name)); }
    @PutMapping("/space-events/{id}") public ResponseEntity<ApiResponse<SpaceEvent>> updateEvent(@PathVariable Long id, @RequestBody String name) { return ResponseEntity.ok(ApiResponse.success(service.updateSpaceEvent(id, name))); }
    @DeleteMapping("/space-events/{id}") public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) { service.deleteSpaceEvent(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/meeting-room-services") public ResponseEntity<ApiResponse<List<MeetingRoomService>>> meeting() { return ResponseEntity.ok(ApiResponse.success(service.getMeetingRoomServices())); }
    @PostMapping("/meeting-room-services") public ResponseEntity<ApiResponse<MeetingRoomService>> addMeeting(@RequestBody ServiceRequest r) { return created(service.addMeetingRoomService(r)); }
    @PutMapping("/meeting-room-services/{id}") public ResponseEntity<ApiResponse<MeetingRoomService>> updateMeeting(@PathVariable Long id, @RequestBody ServiceRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateMeetingRoomService(id, r))); }
    @DeleteMapping("/meeting-room-services/{id}") public ResponseEntity<ApiResponse<Void>> deleteMeeting(@PathVariable Long id) { service.deleteMeetingRoomService(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/pool-services") public ResponseEntity<ApiResponse<List<PoolService>>> pool() { return ResponseEntity.ok(ApiResponse.success(service.getPoolServices())); }
    @PostMapping("/pool-services") public ResponseEntity<ApiResponse<PoolService>> addPool(@RequestBody ServiceRequest r) { return created(service.addPoolService(r)); }
    @PutMapping("/pool-services/{id}") public ResponseEntity<ApiResponse<PoolService>> updatePool(@PathVariable Long id, @RequestBody ServiceRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updatePoolService(id, r))); }
    @DeleteMapping("/pool-services/{id}") public ResponseEntity<ApiResponse<Void>> deletePool(@PathVariable Long id) { service.deletePoolService(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/scopes") public ResponseEntity<ApiResponse<List<ServiceScope>>> scopes() { return ResponseEntity.ok(ApiResponse.success(service.getScopes())); }
    @PostMapping("/scopes") public ResponseEntity<ApiResponse<ServiceScope>> addScope(@RequestBody String code) { return created(service.addScope(code)); }
    @PutMapping("/scopes/{id}") public ResponseEntity<ApiResponse<ServiceScope>> updateScope(@PathVariable Long id, @RequestBody String code) { return ResponseEntity.ok(ApiResponse.success(service.updateScope(id, code))); }
    @DeleteMapping("/scopes/{id}") public ResponseEntity<ApiResponse<Void>> deleteScope(@PathVariable Long id) { service.deleteScope(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/units") public ResponseEntity<ApiResponse<List<Unit>>> units() { return ResponseEntity.ok(ApiResponse.success(service.getUnits())); }
    @PostMapping("/units") public ResponseEntity<ApiResponse<Unit>> addUnit(@RequestBody String name) { return created(service.addUnit(name)); }
    @PutMapping("/units/{id}") public ResponseEntity<ApiResponse<Unit>> updateUnit(@PathVariable Long id, @RequestBody String name) { return ResponseEntity.ok(ApiResponse.success(service.updateUnit(id, name))); }
    @DeleteMapping("/units/{id}") public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable Long id) { service.deleteUnit(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/additional-services") public ResponseEntity<ApiResponse<List<AdditionalService>>> additional() { return ResponseEntity.ok(ApiResponse.success(service.getAdditionalServices())); }
    @PostMapping("/additional-services") public ResponseEntity<ApiResponse<AdditionalService>> addAdditional(@RequestBody AdditionalServiceRequest r) { return created(service.addAdditionalService(r)); }
    @PutMapping("/additional-services/{id}") public ResponseEntity<ApiResponse<AdditionalService>> updateAdditional(@PathVariable Long id, @RequestBody AdditionalServiceRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateAdditionalService(id, r))); }
    @DeleteMapping("/additional-services/{id}") public ResponseEntity<ApiResponse<Void>> deleteAdditional(@PathVariable Long id) { service.deleteAdditionalService(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    private <T> ResponseEntity<ApiResponse<T>> created(T value) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(value)); }
}
