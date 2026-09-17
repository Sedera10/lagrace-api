package mg.lagrace.api.controllers;

import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.services.RoomService;
import mg.lagrace.api.models.views.RoomDetail;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomDetail>>> AllRooms() {
        List<RoomDetail> rooms = roomService.getRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms, "Liste des Chambres"));
    }
    
}
