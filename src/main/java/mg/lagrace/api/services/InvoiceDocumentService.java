package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import mg.lagrace.api.dto.invoice.InvoiceDocumentResponse;
import mg.lagrace.api.dto.invoice.InvoiceLinkResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Invoice;
import mg.lagrace.api.models.InvoiceLine;
import mg.lagrace.api.models.views.InvoiceSummary;
import mg.lagrace.api.repositories.InvoiceLineRepository;
import mg.lagrace.api.repositories.InvoiceRepository;
import mg.lagrace.api.repositories.InvoiceSummaryRepository;

@Service
@Transactional(readOnly = true)
public class InvoiceDocumentService {
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final InvoiceSummaryRepository summaries;
    private final ObjectMapper objectMapper;

    public InvoiceDocumentService(InvoiceRepository invoices, InvoiceLineRepository lines,
            InvoiceSummaryRepository summaries, ObjectMapper objectMapper) {
        this.invoices = invoices;
        this.lines = lines;
        this.summaries = summaries;
        this.objectMapper = objectMapper;
    }

    public InvoiceDocumentResponse get(Long id, String version, boolean copy) {
        String selectedVersion = version == null ? "client" : version.trim().toLowerCase();
        if (!List.of("client", "internal").contains(selectedVersion))
            throw new ValidationException("La version doit être client ou internal.", "version");

        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable avec l'id : " + id));
        InvoiceSummary summary = summaries.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Résumé de facture introuvable."));
        boolean creditNote = "CREDIT_NOTE".equals(invoice.getInvoiceType());
        BigDecimal sign = creditNote ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        List<InvoiceDocumentResponse.Line> documentLines = lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(id)
                .stream().map(line -> line(line, sign)).toList();
        BigDecimal paid = creditNote ? BigDecimal.ZERO : value(summary.getPaidOnInvoice());
        BigDecimal balance = creditNote ? BigDecimal.ZERO : value(summary.getNetTotal()).subtract(paid);
        return new InvoiceDocumentResponse(issuer(invoice.getIssuerSnapshot()), invoice.getInvoiceNumber(),
                invoice.getIssueDate(), creditNote ? "Avoir" : "Facture",
                new InvoiceDocumentResponse.Billing(invoice.getBillingName(), invoice.getBillingAddress(),
                        invoice.getBillingPhone(), invoice.getBillingTaxId()),
                invoice.getReferenceLabel(), new InvoiceDocumentResponse.Period(invoice.getPeriodStart(), invoice.getPeriodEnd()),
                documentLines, invoice.getSubtotal().multiply(sign), invoice.getDiscount().multiply(sign),
                invoice.getTotal().multiply(sign), paid, balance, summary.getPaymentStatus(),
                "internal".equals(selectedVersion) ? invoice.getNotes() : null,
                invoice.getCreditedInvoice() == null ? null : invoice.getCreditedInvoice().getInvoiceNumber(),
                invoices.findAllByCreditedInvoice_IdInvoice(id).stream().map(Invoice::getInvoiceNumber).toList(), copy);
    }

        public List<InvoiceLinkResponse> forBooking(Long bookingId) {
        return invoices.findAllByBooking_IdBookingOrderByIssueDateDescIdInvoiceDesc(bookingId).stream()
            .map(invoice -> new InvoiceLinkResponse(invoice.getIdInvoice(), invoice.getInvoiceNumber(),
                invoice.getIssueDate(), "CREDIT_NOTE".equals(invoice.getInvoiceType())
                    ? invoice.getTotal().negate() : invoice.getTotal(),
                invoice.getInvoiceType(), invoice.getStatus().getCode()))
            .toList();
        }

    private InvoiceDocumentResponse.Line line(InvoiceLine line, BigDecimal sign) {
        return new InvoiceDocumentResponse.Line(line.getDescription(), line.getQuantity(),
                line.getUnitPrice().multiply(sign), value(line.getAmount()).multiply(sign));
    }

    private JsonNode issuer(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Le snapshot légal de la facture est invalide.", exception);
        }
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}