package mg.lagrace.api.billing.web;

import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.employee.Document;
import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.billing.dto.invoice.InvoiceDetailResponse;
import mg.lagrace.api.billing.application.BillingReadService;
import mg.lagrace.api.billing.application.InvoiceIssuanceService;
import mg.lagrace.api.billing.dto.invoice.InvoiceIssueRequest;
import mg.lagrace.api.billing.dto.invoice.FreeInvoiceRequest;
import mg.lagrace.api.billing.dto.invoice.CreditNoteRequest;
import mg.lagrace.api.billing.dto.invoice.CreditDraftResponse;
import mg.lagrace.api.billing.dto.invoice.CreditNoteResponse;
import mg.lagrace.api.billing.dto.invoice.InvoiceDocumentResponse;
import mg.lagrace.api.billing.dto.invoice.InvoiceLinkResponse;
import mg.lagrace.api.billing.application.FreeInvoiceService;
import mg.lagrace.api.billing.application.InvoiceArchiveService;
import mg.lagrace.api.billing.application.InvoiceCreditService;
import mg.lagrace.api.billing.application.InvoiceDocumentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import mg.lagrace.api.auth.application.RequiresPermissions;
@RestController @RequestMapping("/invoices")
public class InvoiceController {
 private final BillingReadService billing; private final InvoiceIssuanceService issuance; private final FreeInvoiceService freeInvoices; private final InvoiceArchiveService archives; private final InvoiceCreditService credits; private final InvoiceDocumentService documents;
 public InvoiceController(BillingReadService billing, InvoiceIssuanceService issuance, FreeInvoiceService freeInvoices, InvoiceArchiveService archives, InvoiceCreditService credits, InvoiceDocumentService documents){this.billing=billing;this.issuance=issuance;this.freeInvoices=freeInvoices;this.archives=archives;this.credits=credits;this.documents=documents;}
 @GetMapping @RequiresPermissions({"INVOICE_READ"}) public ResponseEntity<ApiResponse<?>> list(@RequestParam(required=false) String search,
		 @RequestParam(required=false) String paymentStatus, @RequestParam(required=false) String service,
		 @RequestParam(required=false) String type, @RequestParam(required=false) LocalDate from,
		 @RequestParam(required=false) LocalDate to, @RequestParam(defaultValue="0") int page,
		 @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String sort){
	 return ResponseEntity.ok(ApiResponse.success(archives.list(search,paymentStatus,service,type,from,to,page,size,sort)));
 }
 @GetMapping(value="/export.csv", produces="text/csv") @RequiresPermissions({"INVOICE_READ"}) public ResponseEntity<byte[]> exportCsv(
		 @RequestParam(required=false) String search, @RequestParam(required=false) String paymentStatus,
		 @RequestParam(required=false) String service, @RequestParam(required=false) String type,
		 @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
		 @RequestParam(required=false) String sort){
	 byte[] csv=archives.exportCsv(search,paymentStatus,service,type,from,to,sort);
	 return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
			 .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=archives-factures.csv").body(csv);
 }
 @GetMapping("/{id}") @RequiresPermissions({"INVOICE_READ"}) public ResponseEntity<ApiResponse<InvoiceDetailResponse>> get(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.success(billing.invoiceDetail(id)));}
 @GetMapping("/{id}/document") @RequiresPermissions({"INVOICE_READ"}) public ResponseEntity<ApiResponse<InvoiceDocumentResponse>> document(@PathVariable Long id,
		 @RequestParam(defaultValue="client") String version, @RequestParam(defaultValue="false") boolean copy){
	 return ResponseEntity.ok(ApiResponse.success(documents.get(id,version,copy)));
 }
 @GetMapping("/for-booking/{bookingId}") @RequiresPermissions({"BOOKING_READ", "INVOICE_READ"}) public ResponseEntity<ApiResponse<List<InvoiceLinkResponse>>> forBooking(@PathVariable Long bookingId){
	 return ResponseEntity.ok(ApiResponse.success(documents.forBooking(bookingId)));
 }
 @PostMapping("/preview") @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<InvoiceDetailResponse>> preview(@Valid @RequestBody InvoiceIssueRequest request){return ResponseEntity.ok(ApiResponse.success(issuance.preview(request)));}
 @PostMapping @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<InvoiceDetailResponse>> issue(@Valid @RequestBody InvoiceIssueRequest request){return ResponseEntity.status(201).body(ApiResponse.success(issuance.issue(request)));}
 @PostMapping("/free") @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<InvoiceDetailResponse>> issueFree(@Valid @RequestBody FreeInvoiceRequest request, Authentication authentication){return ResponseEntity.status(201).body(ApiResponse.success(freeInvoices.issue(request, authentication)));}
 @PostMapping("/free/preview") @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<InvoiceDetailResponse>> previewFree(@Valid @RequestBody FreeInvoiceRequest request, Authentication authentication){return ResponseEntity.ok(ApiResponse.success(freeInvoices.preview(request,authentication)));}
 @GetMapping("/{id}/credit-draft") @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<CreditDraftResponse>> creditDraft(@PathVariable Long id, Authentication authentication){return ResponseEntity.ok(ApiResponse.success(credits.draft(id,authentication)));}
 @PostMapping("/{id}/credit-notes") @RequiresPermissions({"INVOICE_ISSUE"}) public ResponseEntity<ApiResponse<CreditNoteResponse>> credit(@PathVariable Long id, @Valid @RequestBody CreditNoteRequest request, Authentication authentication){return ResponseEntity.status(201).body(ApiResponse.success(credits.credit(id,request,authentication)));}
}
