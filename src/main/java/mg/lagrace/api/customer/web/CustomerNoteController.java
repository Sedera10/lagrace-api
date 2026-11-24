package mg.lagrace.api.customer.web;

import mg.lagrace.api.customer.Customer;

import jakarta.validation.Valid;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.customer.dto.CustomerMutationRequests;
import mg.lagrace.api.customer.dto.CustomerNoteResponse;
import mg.lagrace.api.customer.application.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer-notes")
public class CustomerNoteController {
    private final CustomerService customerService;

    public CustomerNoteController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> update(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.NoteUpdate request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.updateNote(id, request, authentication)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> archive(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.NoteVersion request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.archiveNote(id, request, false, authentication)));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> restore(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.NoteVersion request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(customerService.archiveNote(id, request, true, authentication)));
    }
}