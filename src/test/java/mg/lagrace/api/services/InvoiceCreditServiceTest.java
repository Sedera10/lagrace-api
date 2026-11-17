package mg.lagrace.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import mg.lagrace.api.dto.invoice.CreditNoteRequest;
import mg.lagrace.api.dto.invoice.InvoiceDetailResponse;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.Invoice;
import mg.lagrace.api.models.InvoiceLine;
import mg.lagrace.api.models.InvoiceStatus;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.views.InvoiceSummary;
import mg.lagrace.api.repositories.HotelSettingsRepository;
import mg.lagrace.api.repositories.InvoiceLineRepository;
import mg.lagrace.api.repositories.InvoiceRepository;
import mg.lagrace.api.repositories.InvoiceStatusRepository;
import mg.lagrace.api.repositories.InvoiceSummaryRepository;
import mg.lagrace.api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class InvoiceCreditServiceTest {
    @Mock private InvoiceRepository invoices;
    @Mock private InvoiceLineRepository lines;
    @Mock private InvoiceStatusRepository statuses;
    @Mock private InvoiceSummaryRepository summaries;
    @Mock private HotelSettingsRepository settings;
    @Mock private UserRepository users;
    @Mock private BookingChangeLogger changes;
    @Mock private BillingReadService billingRead;

    private InvoiceCreditService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceCreditService(invoices, lines, statuses, summaries, settings, users, changes,
                billingRead, Clock.fixed(Instant.parse("2026-11-11T09:00:00Z"), ZoneId.of("Indian/Antananarivo")));
    }

    @Test
    void selectingAllPositiveLinesAutomaticallyCreditsDiscountAndSuggestsRefund() {
        User user = new User();
        user.setIdUser(7L);
        when(users.findByUsername("manager")).thenReturn(Optional.of(user));

        InvoiceStatus issued = new InvoiceStatus();
        issued.setCode("ISSUED");
        InvoiceStatus credited = new InvoiceStatus();
        credited.setCode("CREDITED");
        when(statuses.findByCode("ISSUED")).thenReturn(Optional.of(issued));
        when(statuses.findByCode("CREDITED")).thenReturn(Optional.of(credited));

        Booking booking = new Booking();
        booking.setIdBooking(55L);
        Invoice original = new Invoice();
        ReflectionTestUtils.setField(original, "idInvoice", 12L);
        ReflectionTestUtils.setField(original, "invoiceNumber", "FAC-2026-000012");
        original.setInvoiceType("INVOICE");
        original.setBooking(booking);
        original.setStatus(issued);
        original.setSubtotal(new BigDecimal("80.00"));
        original.setDiscount(new BigDecimal("20.00"));
        original.setTotal(new BigDecimal("60.00"));
        original.setBillingName("Client Exemple");
        original.setIssueDate(LocalDate.of(2026, 11, 10));
        when(invoices.findByIdForUpdate(12L)).thenReturn(Optional.of(original));

        InvoiceLine positive = line(101L, "Prestation", "80.00", true);
        InvoiceLine discount = line(102L, "Remise", "-20.00", true);
        positive.setInvoice(original);
        discount.setInvoice(original);
        when(lines.findAllByInvoice_IdInvoiceOrderByPositionAsc(12L)).thenReturn(List.of(positive, discount));

        InvoiceSummary before = summary(new BigDecimal("60.00"), new BigDecimal("60.00"));
        InvoiceSummary after = summary(BigDecimal.ZERO, BigDecimal.ZERO);
        when(summaries.findById(12L)).thenReturn(Optional.of(before), Optional.of(after));
        when(settings.findById(1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(mg.lagrace.api.models.HotelSettings.class)));
        when(invoices.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            if ("CREDIT_NOTE".equals(invoice.getInvoiceType())) {
                ReflectionTestUtils.setField(invoice, "idInvoice", 88L);
                ReflectionTestUtils.setField(invoice, "invoiceNumber", "AV-2026-000001");
            }
            return invoice;
        });
        InvoiceDetailResponse response = new InvoiceDetailResponse(88L, "AV-2026-000001", "CREDIT_NOTE", "ISSUED",
                LocalDate.of(2026, 11, 11), 55L, null, "Client Exemple", null, null, null, null, null,
                null, null, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("60.00"), null, "{}",
                "CREDIT_NOTE", BigDecimal.ZERO, BigDecimal.ZERO, "FAC-2026-000012", List.of(), List.of(), null);
        when(billingRead.invoiceDetail(88L)).thenReturn(response);

        var authentication = new UsernamePasswordAuthenticationToken("manager", "",
                List.of(new SimpleGrantedAuthority("ROLE_Responsable")));
        var result = service.credit(12L, new CreditNoteRequest(List.of(101L), "Service annulé", new BigDecimal("60.00")), authentication);

        assertEquals(new BigDecimal("60.00"), result.refundSuggested());
        assertEquals("CREDITED", original.getStatus().getCode());
        assertEquals(false, positive.getBilled());
        assertEquals(false, discount.getBilled());
    ArgumentCaptor<InvoiceLine> captured = ArgumentCaptor.forClass(InvoiceLine.class);
    org.mockito.Mockito.verify(lines, org.mockito.Mockito.times(4)).save(captured.capture());
    List<InvoiceLine> creditLines = captured.getAllValues().stream()
        .filter(line -> line.getInvoice() != original).toList();
    assertEquals(List.of("Avoir : Prestation", "Avoir : Remise"), creditLines.stream()
        .map(InvoiceLine::getDescription).toList());
    assertEquals(null, creditLines.get(0).getSourceId());
    verify(changes).log(org.mockito.ArgumentMatchers.eq(55L), org.mockito.ArgumentMatchers.eq("INVOICE"),
        org.mockito.ArgumentMatchers.eq(12L), org.mockito.ArgumentMatchers.eq("CREDIT"),
        org.mockito.ArgumentMatchers.eq("credit_note"), org.mockito.ArgumentMatchers.isNull(),
        org.mockito.ArgumentMatchers.eq("AV-2026-000001"), org.mockito.ArgumentMatchers.eq("Service annulé"),
        org.mockito.ArgumentMatchers.eq(7L), any());
    }

    private InvoiceLine line(Long id, String description, String amount, boolean billed) {
    InvoiceLine line = new InvoiceLine();
    ReflectionTestUtils.setField(line, "idInvoiceLine", id);
    line.setDescription(description);
    line.setQuantity(BigDecimal.ONE);
    line.setUnitPrice(new BigDecimal(amount));
    ReflectionTestUtils.setField(line, "amount", new BigDecimal(amount));
    line.setBilled(billed);
    return line;
    }

    private InvoiceSummary summary(BigDecimal net, BigDecimal paid) {
    InvoiceSummary summary = new InvoiceSummary();
    ReflectionTestUtils.setField(summary, "idInvoice", 12L);
    ReflectionTestUtils.setField(summary, "netTotal", net);
    ReflectionTestUtils.setField(summary, "paidOnInvoice", paid);
    ReflectionTestUtils.setField(summary, "paymentStatus", "PARTIAL");
    return summary;
    }
}
