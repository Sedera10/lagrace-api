package mg.lagrace.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import mg.lagrace.api.dto.invoice.FreeInvoiceRequest;
import mg.lagrace.api.dto.invoice.InvoiceDetailResponse;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingOther;
import mg.lagrace.api.models.BookingStatus;
import mg.lagrace.api.models.Customer;
import mg.lagrace.api.models.HotelSettings;
import mg.lagrace.api.models.Invoice;
import mg.lagrace.api.models.InvoiceLine;
import mg.lagrace.api.models.InvoiceStatus;
import mg.lagrace.api.models.PaymentKind;
import mg.lagrace.api.models.PaymentMethod;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.BookingOtherRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingStatusHistoryRepository;
import mg.lagrace.api.repositories.BookingStatusRepository;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.HotelSettingsRepository;
import mg.lagrace.api.repositories.InvoiceLineRepository;
import mg.lagrace.api.repositories.InvoiceRepository;
import mg.lagrace.api.repositories.InvoiceStatusRepository;
import mg.lagrace.api.repositories.PaymentKindRepository;
import mg.lagrace.api.repositories.PaymentMethodRepository;
import mg.lagrace.api.repositories.PaymentRepository;
import mg.lagrace.api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class FreeInvoiceServiceTest {
    @Mock private BookingRepository bookings;
    @Mock private BookingStatusRepository bookingStatuses;
    @Mock private BookingStatusHistoryRepository bookingHistory;
    @Mock private BookingOtherRepository others;
    @Mock private CustomerRepository customers;
    @Mock private UserRepository users;
    @Mock private InvoiceRepository invoices;
    @Mock private InvoiceLineRepository invoiceLines;
    @Mock private InvoiceStatusRepository invoiceStatuses;
    @Mock private HotelSettingsRepository settings;
    @Mock private PaymentRepository payments;
    @Mock private PaymentKindRepository paymentKinds;
    @Mock private PaymentMethodRepository paymentMethods;
    @Mock private BookingStatusService statusService;
    @Mock private BookingChangeLogger changeLogger;
    @Mock private BillingReadService billingRead;

    private FreeInvoiceService service;

    @BeforeEach
    void setUp() {
        service = new FreeInvoiceService(bookings, bookingStatuses, bookingHistory, others, customers, users,
                invoices, invoiceLines, invoiceStatuses, settings, payments, paymentKinds, paymentMethods,
                statusService, changeLogger, billingRead,
                Clock.fixed(Instant.parse("2026-11-11T09:00:00Z"), ZoneId.of("Indian/Antananarivo")));
        ReflectionTestUtils.setField(service, "walkInCustomerId", 1L);
        ReflectionTestUtils.setField(service, "maxOtherAmount", new BigDecimal("500000.00"));
    }

    @Test
    void paidFreeInvoiceCreatesWalkInBookingItemsInvoiceAndClosesBooking() {
        User user = new User();
        user.setIdUser(8L);
        when(users.findByUsername("reception")).thenReturn(Optional.of(user));

        Customer walkIn = new Customer();
        walkIn.setIdCustomer(1L);
        walkIn.setLastName("Client de passage");
        when(customers.findById(1L)).thenReturn(Optional.of(walkIn));

        BookingStatus inProgress = new BookingStatus();
        inProgress.setCode("IN_PROGRESS");
        when(bookingStatuses.findByCode("IN_PROGRESS")).thenReturn(inProgress);
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setIdBooking(44L);
            return booking;
        });

        AtomicLong otherId = new AtomicLong(100L);
        when(others.saveAndFlush(any(BookingOther.class))).thenAnswer(invocation -> {
            BookingOther other = invocation.getArgument(0);
            other.setIdBookingOther(otherId.getAndIncrement());
            return other;
        });
        when(invoiceStatuses.findByCode("ISSUED")).thenReturn(Optional.of(new InvoiceStatus()));
        when(settings.findById(1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(HotelSettings.class)));
        when(invoices.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "idInvoice", 77L);
            return invoice;
        });
        when(paymentMethods.findById(3L)).thenReturn(Optional.of(org.mockito.Mockito.mock(PaymentMethod.class)));
        when(paymentKinds.findByCode("PAYMENT")).thenReturn(Optional.of(org.mockito.Mockito.mock(PaymentKind.class)));

        InvoiceDetailResponse response = new InvoiceDetailResponse(77L, "FAC-2026-000001", "INVOICE", "ISSUED",
                java.time.LocalDate.of(2026, 11, 11), 44L, 1L, "Client facturé", null, null, null, null, null,
                null, null, new BigDecimal("120.00"), BigDecimal.ZERO, new BigDecimal("120.00"), null, "{}",
                "PAID", new BigDecimal("120.00"), BigDecimal.ZERO, null, List.of(), List.of(), null);
        when(billingRead.invoiceDetail(77L)).thenReturn(response);

        FreeInvoiceRequest request = new FreeInvoiceRequest(
                new FreeInvoiceRequest.Customer("Client facturé", "0340000000", "Malagasy", "Antananarivo"),
                "Chambre double", java.time.LocalDate.of(2026, 11, 11), java.time.LocalDate.of(2026, 11, 12),
                List.of(new FreeInvoiceRequest.Line("ROOM", "Chambre double", new BigDecimal("1.00"), new BigDecimal("120.00"))),
                BigDecimal.ZERO, new FreeInvoiceRequest.Payment("PAID", 3L, null), "Test facture libre", new BigDecimal("120.00"));
        var authentication = new UsernamePasswordAuthenticationToken("reception", "",
                List.of(new SimpleGrantedAuthority("ROLE_Receptionniste")));

        InvoiceDetailResponse result = service.issue(request, authentication);

        assertSame(response, result);
        ArgumentCaptor<InvoiceLine> invoiceLine = ArgumentCaptor.forClass(InvoiceLine.class);
        verify(invoiceLines).save(invoiceLine.capture());
        assertEquals("ROOM", invoiceLine.getValue().getServiceCode());
        assertEquals(new BigDecimal("1.00"), invoiceLine.getValue().getQuantity());
        assertEquals(new BigDecimal("120.00"), invoiceLine.getValue().getUnitPrice());
        verify(payments).saveAndFlush(any());
        verify(statusService).changeBookingStatus(any(Booking.class), eq("CLOSED"), eq("Facture payée intégralement"),
                eq(8L), any());
        verify(bookingHistory).save(any());
    }
}