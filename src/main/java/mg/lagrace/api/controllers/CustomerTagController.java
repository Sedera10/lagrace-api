package mg.lagrace.api.controllers;

import java.util.List;
import jakarta.validation.Valid;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.customer.CustomerMutationRequests;
import mg.lagrace.api.dto.customer.CustomerTagResponse;
import mg.lagrace.api.services.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer-tags")
public class CustomerTagController {
    private final CustomerService customerService;

    public CustomerTagController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerTagResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(customerService.customerTags()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerTagResponse>> create(@Valid @RequestBody CustomerMutationRequests.CustomerTag request,
            Authentication authentication) {
        requireResponsible(authentication);
        return ResponseEntity.status(201).body(ApiResponse.success(customerService.createTag(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerTagResponse>> update(@PathVariable Long id,
            @Valid @RequestBody CustomerMutationRequests.CustomerTag request, Authentication authentication) {
        requireResponsible(authentication);
        return ResponseEntity.ok(ApiResponse.success(customerService.updateTag(id, request)));
    }

    private void requireResponsible(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(authority -> {
            String name = authority.getAuthority();
            return name.contains("Responsable") || name.contains("Administrateur");
        })) throw new AccessDeniedException("Administration des étiquettes réservée au responsable.");
    }
}