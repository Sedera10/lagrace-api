package mg.lagrace.api.services;

import mg.lagrace.api.employee.Document;

import mg.lagrace.api.billing.application.InvoiceDocumentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;
import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.billing.InvoiceLine;
import mg.lagrace.api.billing.InvoiceStatus;
import mg.lagrace.api.billing.InvoiceSummary;
import mg.lagrace.api.billing.persistence.InvoiceLineRepository;
import mg.lagrace.api.billing.persistence.InvoiceRepository;
import mg.lagrace.api.billing.persistence.InvoiceSummaryRepository;

@ExtendWith(MockitoExtension.class)
class InvoiceDocumentServiceTest {
    @Mock private InvoiceRepository invoices;
    @Mock private InvoiceLineRepository lines;
    @Mock private InvoiceSummaryRepository summaries;

    private InvoiceDocumentService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceDocumentService(invoices, lines, summaries, new ObjectMapper());
    }

    @Test
    void clientDocumentNeverContainsNotesAndCanBeMarkedAsCopy() {
        Invoice invoice = invoice("INVOICE");
        InvoiceSummary summary = summary("UNPAID");
        InvoiceLine line = line("Séjour chambre", "120.00");
        when(invoices.findById(12L)).thenReturn(Optional.of(invoice));
        when(summaries.findById(12L)).thenReturn(Optional.of(summary));
        when(lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(12L)).thenReturn(List.of(line));
        when(invoices.findAllByCreditedInvoice_IdInvoice(12L)).thenReturn(List.of());

        var document = service.get(12L, "client", true);

        assertNull(document.notes());
        assertTrue(document.isCopy());
        assertEquals("FAC-2026-000012", document.number());
        assertEquals("Hôtel La Grâce", document.issuer().get("legalName").asText());
        assertEquals(new BigDecimal("120.00"), document.lines().get(0).amount());
    }

    @Test
    void internalDocumentIncludesNotes() {
        Invoice invoice = invoice("INVOICE");
        InvoiceSummary summary = summary("PARTIAL");
        when(invoices.findById(12L)).thenReturn(Optional.of(invoice));
        when(summaries.findById(12L)).thenReturn(Optional.of(summary));
        when(lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(12L)).thenReturn(List.of());
        when(invoices.findAllByCreditedInvoice_IdInvoice(12L)).thenReturn(List.of());

        var document = service.get(12L, "internal", false);

        assertEquals("Détail confidentiel", document.notes());
        assertEquals(new BigDecimal("80.00"), document.paid());
        assertEquals(new BigDecimal("40.00"), document.balance());
    }

    private Invoice invoice(String type) {
        Invoice invoice = new Invoice();
        ReflectionTestUtils.setField(invoice, "idInvoice", 12L);
        invoice.setInvoiceType(type);
        ReflectionTestUtils.setField(invoice, "invoiceNumber", "FAC-2026-000012");
        invoice.setIssueDate(LocalDate.of(2026, 11, 11));
        invoice.setBillingName("Client Exemple");
        invoice.setSubtotal(new BigDecimal("120.00"));
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal("120.00"));
        invoice.setNotes("Détail confidentiel");
        invoice.setIssuerSnapshot("{\"legalName\":\"Hôtel La Grâce\"}");
        InvoiceStatus status = new InvoiceStatus();
        status.setCode("ISSUED");
        invoice.setStatus(status);
        return invoice;
    }

    private InvoiceSummary summary(String paymentStatus) {
        InvoiceSummary summary = new InvoiceSummary();
        ReflectionTestUtils.setField(summary, "idInvoice", 12L);
        ReflectionTestUtils.setField(summary, "netTotal", new BigDecimal("120.00"));
        ReflectionTestUtils.setField(summary, "paidOnInvoice", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(summary, "paymentStatus", paymentStatus);
        return summary;
    }

    private InvoiceLine line(String description, String amount) {
        InvoiceLine line = new InvoiceLine();
        line.setDescription(description);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal(amount));
        ReflectionTestUtils.setField(line, "amount", new BigDecimal(amount));
        return line;
    }
}