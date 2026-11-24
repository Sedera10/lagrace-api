package mg.lagrace.api.booking.configuration.web;

import jakarta.validation.Valid;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import mg.lagrace.api.billing.InvoiceStatus;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.booking.dto.BookingStatusRequest;
import mg.lagrace.api.booking.configuration.dto.CodeNameRequest;
import mg.lagrace.api.payment.PaymentMethod;
import mg.lagrace.api.booking.application.BookingStatusService;
import mg.lagrace.api.booking.configuration.application.ReservationConfigService;

@RestController
@RequestMapping("/booking-configs")
public class ReservationConfigController {
    private final ReservationConfigService service;
    private final BookingStatusService service2;
    public ReservationConfigController(ReservationConfigService service, BookingStatusService service2) { 
        this.service = service; 
        this.service2 = service2;
    }

    @GetMapping("/statuses") public ResponseEntity<ApiResponse<List<BookingStatus>>> statuses() { return ResponseEntity.ok(ApiResponse.success(service2.getAllBokingStatus())); }
    @PostMapping("/statuses") public ResponseEntity<ApiResponse<BookingStatus>> addStatus(@Valid @RequestBody BookingStatusRequest r) { return created(service2.addBookingStatus(r)); }
    @PutMapping("/statuses/{id}") public ResponseEntity<ApiResponse<BookingStatus>> updateStatus(@PathVariable Long id, @Valid @RequestBody BookingStatusRequest r) { return ResponseEntity.ok(ApiResponse.success(service2.updateBookingStatus(id, r))); }
    @DeleteMapping("/statuses/{id}") public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long id) { service2.deleteBookingStatus(id); return ResponseEntity.ok(ApiResponse.success(null)); }

    @GetMapping("/line-statuses") public ResponseEntity<ApiResponse<List<BookingLineStatus>>> lineStatuses() { return ResponseEntity.ok(ApiResponse.success(service2.getAllBokingLineStatus())); }
    @PostMapping("/line-statuses") public ResponseEntity<ApiResponse<BookingLineStatus>> addLineStatus(@Valid @RequestBody BookingStatusRequest r) { return created(service2.addBookingLineStatus(r)); }
    @PutMapping("/line-statuses/{id}") public ResponseEntity<ApiResponse<BookingLineStatus>> updateLineStatus(@PathVariable Long id, @Valid @RequestBody BookingStatusRequest r) { return ResponseEntity.ok(ApiResponse.success(service2.updateBookingLineStatus(id, r))); }
    @DeleteMapping("/line-statuses/{id}") public ResponseEntity<ApiResponse<Void>> deleteLineStatus(@PathVariable Long id) { service2.deleteBookingLineStatus(id); return ResponseEntity.ok(ApiResponse.success(null)); }

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
