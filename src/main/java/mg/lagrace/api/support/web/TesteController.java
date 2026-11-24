package mg.lagrace.api.support.web;

import mg.lagrace.api.shared.api.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class TesteController {
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<Void>> Teste() {
        return ResponseEntity.ok(ApiResponse.success(
                null, "Teste teste teste"));
    }
}
