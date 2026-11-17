package mg.lagrace.api.controllers;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.HotelSettings;
import mg.lagrace.api.repositories.HotelSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/hotel-settings")
public class HotelSettingsController {
 private final HotelSettingsRepository settings;
 public HotelSettingsController(HotelSettingsRepository settings){this.settings=settings;}
 @GetMapping public ResponseEntity<ApiResponse<HotelSettings>> get(){return ResponseEntity.ok(ApiResponse.success(settings.findById(1L).orElseThrow(()->new ResourceNotFoundException("Paramètres de l'hôtel introuvables."))));}
}
