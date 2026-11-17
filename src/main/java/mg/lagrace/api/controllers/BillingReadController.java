package mg.lagrace.api.controllers;
import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.invoice.BillableItemsResponse;
import mg.lagrace.api.services.BillingReadService;
import mg.lagrace.api.services.InvoiceIssuanceService;
import mg.lagrace.api.dto.invoice.DiscountRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/bookings")
public class BillingReadController {
 private final BillingReadService billing; private final InvoiceIssuanceService issuance;
 public BillingReadController(BillingReadService billing, InvoiceIssuanceService issuance){this.billing=billing;this.issuance=issuance;}
 @GetMapping("/{bookingId}/billable-items") public ResponseEntity<ApiResponse<BillableItemsResponse>> billableItems(@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.success(billing.billableItems(bookingId)));}
 @GetMapping("/{bookingId}/invoice-draft") public ResponseEntity<ApiResponse<BillableItemsResponse>> draft(@PathVariable Long bookingId){return ResponseEntity.ok(ApiResponse.success(issuance.draft(bookingId)));}
 @PostMapping("/{bookingId}/discounts") public ResponseEntity<ApiResponse<BillableItemsResponse>> discount(@PathVariable Long bookingId,@Valid @RequestBody DiscountRequest request,Authentication authentication){if(authentication.getAuthorities().stream().noneMatch(a->a.getAuthority().contains("Responsable")||a.getAuthority().contains("Administrateur")))throw new org.springframework.security.access.AccessDeniedException("Remise réservée au responsable.");return ResponseEntity.ok(ApiResponse.success(issuance.addDiscount(bookingId,request)));}
}
