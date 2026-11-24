package mg.lagrace.api.billing.web;

import mg.lagrace.api.auth.application.RequiresPermissions;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.billing.dto.invoice.BillableItemsResponse;
import mg.lagrace.api.billing.application.BillingReadService;
import mg.lagrace.api.billing.application.InvoiceIssuanceService;
import mg.lagrace.api.billing.dto.invoice.DiscountRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BillingReadController {
    private final BillingReadService billing;
    private final InvoiceIssuanceService issuance;

    public BillingReadController(BillingReadService billing, InvoiceIssuanceService issuance) {
        this.billing = billing;
        this.issuance = issuance;
    }

    @GetMapping("/{bookingId}/billable-items")
    @RequiresPermissions({"BOOKING_READ", "INVOICE_READ"})
    public ResponseEntity<ApiResponse<BillableItemsResponse>> billableItems(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(billing.billableItems(bookingId)));
    }

    @GetMapping("/{bookingId}/invoice-draft")
    @RequiresPermissions({"BOOKING_READ", "INVOICE_READ"})
    public ResponseEntity<ApiResponse<BillableItemsResponse>> draft(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.success(issuance.draft(bookingId)));
    }

    @PostMapping("/{bookingId}/discounts")
    @RequiresPermissions({"BOOKING_READ", "OTHER_DISCOUNT"})
    public ResponseEntity<ApiResponse<BillableItemsResponse>> discount(@PathVariable Long bookingId,
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(issuance.addDiscount(bookingId, request)));
    }
}
