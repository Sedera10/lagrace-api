package mg.lagrace.api.leave.web;
import java.time.LocalDate; import java.util.List;
import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import mg.lagrace.api.leave.application.LeaveService; import mg.lagrace.api.leave.dto.LeaveDtos.*; import mg.lagrace.api.shared.api.ApiResponse;
@RestController public class LeaveController { private final LeaveService service; public LeaveController(LeaveService s){service=s;}
 @GetMapping("/leave-types") public ApiResponse<List<TypeResponse>> types(){return ApiResponse.success(service.types());}
 @PostMapping("/leave-types") public ResponseEntity<ApiResponse<TypeResponse>> createType(@RequestBody TypeRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.createType(r)));}
 @PatchMapping("/leave-types/{id}") public ApiResponse<TypeResponse> renameType(@PathVariable Long id,@RequestBody TypeRequest r){return ApiResponse.success(service.renameType(id,r));}
 @DeleteMapping("/leave-types/{id}") public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id){service.deleteType(id);return ResponseEntity.ok(ApiResponse.success(null));}
 @GetMapping("/leaves") public ApiResponse<Page> list(@RequestParam(required=false)Long employeeId,@RequestParam(required=false)Long typeId,@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(required=false)String state,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(required=false)String sort){return ApiResponse.success(service.list(employeeId,typeId,from,to,state,page,size,sort));}
 @GetMapping("/leaves/{id}") public ApiResponse<Item> one(@PathVariable Long id){return ApiResponse.success(service.one(id));}
 @PostMapping("/leaves") public ResponseEntity<ApiResponse<Item>> create(@RequestBody Create r,Authentication a){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(r,a)));}
 @PatchMapping("/leaves/{id}") public ApiResponse<Item> update(@PathVariable Long id,@RequestBody Update r){return ApiResponse.success(service.update(id,r));}
 @DeleteMapping("/leaves/{id}") public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id){service.delete(id);return ResponseEntity.ok(ApiResponse.success(null));}
 @GetMapping("/leaves/overview") public ApiResponse<List<Overview>> overview(@RequestParam(required=false)LocalDate date){return ApiResponse.success(service.overview(date));}
}
