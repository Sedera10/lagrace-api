package mg.lagrace.api.customer.web;

import mg.lagrace.api.customer.Customer;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;

import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.customer.dto.CustomerResponse;
import mg.lagrace.api.customer.dto.CustomerFileResponse;
import mg.lagrace.api.customer.dto.CustomerAlertsResponse;
import mg.lagrace.api.customer.dto.CustomerBookingsResponse;
import mg.lagrace.api.customer.dto.CustomerInvoicesResponse;
import mg.lagrace.api.customer.dto.CustomerPaymentsResponse;
import mg.lagrace.api.customer.dto.CustomerListResponse;
import mg.lagrace.api.customer.dto.CustomerMutationRequests;
import mg.lagrace.api.customer.dto.CustomerNoteResponse;
import mg.lagrace.api.customer.dto.CustomerSearchResponse;
import mg.lagrace.api.customer.dto.CustomerDuplicateRequest;
import mg.lagrace.api.customer.dto.CustomerDuplicateResponse;
import mg.lagrace.api.customer.dto.CustomerChangesResponse;
import mg.lagrace.api.customer.application.CustomerService;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CustomerSearchResponse>>> searchCustomers(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(customerService.search(query, limit)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/check-duplicates")
    public ResponseEntity<ApiResponse<CustomerDuplicateResponse>> checkDuplicates(
            @Valid @org.springframework.web.bind.annotation.RequestBody CustomerDuplicateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.checkDuplicates(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerListResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Boolean hasBalance,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(customerService.list(search, tagId, hasBalance, active, page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> customerFile(@PathVariable Long id) {
        var result = customerService.file(id);
        if (result.mergedInto() != null) {
            ApiResponse<Object> response = ApiResponse.apiError("CUSTOMER_MERGED", "Cette fiche a été fusionnée.");
            response.setData(java.util.Map.of("mergedInto", result.mergedInto()));
            return ResponseEntity.status(409).body(response);
        }
        return ResponseEntity.ok(ApiResponse.success(result.file()));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<ApiResponse<CustomerAlertsResponse>> alerts(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.alerts(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerFileResponse>> update(@PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody CustomerMutationRequests.Update request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.update(id, request, authentication)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CustomerFileResponse>> archive(@PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody(required = false) CustomerMutationRequests.Reason request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.archive(id, request, authentication)));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CustomerFileResponse>> restore(@PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.restore(id, authentication)));
    }

    @PostMapping("/{keptId}/merge")
    public ResponseEntity<ApiResponse<CustomerService.MergeResult>> merge(@PathVariable Long keptId,
            @Valid @org.springframework.web.bind.annotation.RequestBody CustomerMutationRequests.Merge request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.merge(keptId, request, authentication)));
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<CustomerNoteResponse>>> notes(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean archived) {
        return ResponseEntity.ok(ApiResponse.success(customerService.notes(id, archived)));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> addNote(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.NoteCreate request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(ApiResponse.success(customerService.addNote(id, request, authentication)));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<ApiResponse<CustomerFileResponse>> replaceTags(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.TagReplace request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.replaceTags(id, request, authentication)));
    }

    @GetMapping("/{id}/changes")
    public ResponseEntity<ApiResponse<CustomerChangesResponse>> changes(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(customerService.changes(id, page, size)));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<CustomerBookingsResponse>> bookings(@PathVariable Long id,
            @RequestParam(required = false) String status, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to, @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(customerService.bookings(id, status, from, to, service, page, size, sort)));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<ApiResponse<CustomerInvoicesResponse>> invoices(@PathVariable Long id,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(customerService.invoices(id, from, to, page, size)));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<CustomerPaymentsResponse>> payments(@PathVariable Long id,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(customerService.payments(id, from, to, page, size)));
    }
}
