package mg.lagrace.api.controllers;

import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.customer.CustomerMutationRequests;
import mg.lagrace.api.dto.customer.CustomerNoteResponse;
import mg.lagrace.api.services.CustomerService;
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