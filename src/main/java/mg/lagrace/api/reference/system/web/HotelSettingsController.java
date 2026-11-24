package mg.lagrace.api.reference.system.web;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.reference.HotelSettings;
import mg.lagrace.api.reference.system.application.HotelSettingsService;
import mg.lagrace.api.reference.system.dto.HotelSettingsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/hotel-settings")
public class HotelSettingsController {
    private final HotelSettingsService settings;

    public HotelSettingsController(HotelSettingsService settings){
        this.settings=settings;
    }
    @GetMapping public ResponseEntity<ApiResponse<HotelSettings>> get(){
        return ResponseEntity.ok(ApiResponse.success(settings.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<HotelSettings>> update(@Valid @RequestBody HotelSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settings.update(request), "Paramètres de l'hôtel mis à jour."));
    }
}
