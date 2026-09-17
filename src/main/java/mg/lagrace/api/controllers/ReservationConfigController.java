package mg.lagrace.api.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.reservation.CodeNameRequest;
import mg.lagrace.api.dto.reservation.ReservationStatusRequest;
import mg.lagrace.api.models.*;
import mg.lagrace.api.services.ReservationConfigService;

@RestController
@RequestMapping("/reservation-configs")
public class ReservationConfigController {
    private final ReservationConfigService service;
    public ReservationConfigController(ReservationConfigService service) { this.service = service; }

    @GetMapping("/statuses") public ResponseEntity<ApiResponse<List<ReservationStatus>>> statuses() { return ResponseEntity.ok(ApiResponse.success(service.getReservationStatuses())); }
    @PostMapping("/statuses") public ResponseEntity<ApiResponse<ReservationStatus>> addStatus(@RequestBody ReservationStatusRequest r) { return created(service.addReservationStatus(r)); }
    @PutMapping("/statuses/{id}") public ResponseEntity<ApiResponse<ReservationStatus>> updateStatus(@PathVariable Long id, @RequestBody ReservationStatusRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateReservationStatus(id, r))); }
    @DeleteMapping("/statuses/{id}") public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long id) { service.deleteReservationStatus(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/payment-methods") public ResponseEntity<ApiResponse<List<PaymentMethod>>> methods() { return ResponseEntity.ok(ApiResponse.success(service.getPaymentMethods())); }
    @PostMapping("/payment-methods") public ResponseEntity<ApiResponse<PaymentMethod>> addMethod(@RequestBody CodeNameRequest r) { return created(service.addPaymentMethod(r)); }
    @PutMapping("/payment-methods/{id}") public ResponseEntity<ApiResponse<PaymentMethod>> updateMethod(@PathVariable Long id, @RequestBody CodeNameRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updatePaymentMethod(id, r))); }
    @DeleteMapping("/payment-methods/{id}") public ResponseEntity<ApiResponse<Void>> deleteMethod(@PathVariable Long id) { service.deletePaymentMethod(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/invoice-statuses") public ResponseEntity<ApiResponse<List<InvoiceStatus>>> invoices() { return ResponseEntity.ok(ApiResponse.success(service.getInvoiceStatuses())); }
    @PostMapping("/invoice-statuses") public ResponseEntity<ApiResponse<InvoiceStatus>> addInvoice(@RequestBody CodeNameRequest r) { return created(service.addInvoiceStatus(r)); }
    @PutMapping("/invoice-statuses/{id}") public ResponseEntity<ApiResponse<InvoiceStatus>> updateInvoice(@PathVariable Long id, @RequestBody CodeNameRequest r) { return ResponseEntity.ok(ApiResponse.success(service.updateInvoiceStatus(id, r))); }
    @DeleteMapping("/invoice-statuses/{id}") public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) { service.deleteInvoiceStatus(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    private <T> ResponseEntity<ApiResponse<T>> created(T value) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(value)); }
}
