package mg.lagrace.api.services;

import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.payment.Payment;

import mg.lagrace.api.billing.application.BillingReadService;
import mg.lagrace.api.billing.application.FreeInvoiceService;
import mg.lagrace.api.booking.application.BookingChangeLogger;
import mg.lagrace.api.booking.application.BookingStatusService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.core.BookingStatusHistoryRepository;
import mg.lagrace.api.booking.core.BookingStatusRepository;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.billing.dto.invoice.FreeInvoiceRequest;
import mg.lagrace.api.billing.dto.invoice.InvoiceDetailResponse;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.billing.InvoiceLine;
import mg.lagrace.api.billing.InvoiceStatus;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.payment.PaymentKind;
import mg.lagrace.api.payment.PaymentMethod;
import mg.lagrace.api.reference.HotelSettings;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.customer.persistence.CustomerRepository;
import mg.lagrace.api.customer.persistence.IdentificationDocRepository;
import mg.lagrace.api.reference.persistence.NationalityRepository;
import mg.lagrace.api.reference.system.persistence.HotelSettingsRepository;
import mg.lagrace.api.billing.persistence.InvoiceLineRepository;
import mg.lagrace.api.billing.persistence.InvoiceRepository;
import mg.lagrace.api.billing.persistence.InvoiceStatusRepository;
import mg.lagrace.api.payment.persistence.PaymentKindRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;
import mg.lagrace.api.payment.persistence.PaymentRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class FreeInvoiceServiceTest {
    @Mock private BookingRepository bookings;
    @Mock private BookingStatusRepository bookingStatuses;
    @Mock private BookingStatusHistoryRepository bookingHistory;
    @Mock private BookingOtherRepository others;
    @Mock private CustomerRepository customers;
    @Mock private IdentificationDocRepository documents;
    @Mock private NationalityRepository nationalities;
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
                statusService, changeLogger, billingRead, documents, nationalities,
                Clock.fixed(Instant.parse("2026-11-11T09:00:00Z"), ZoneId.of("Indian/Antananarivo")));
        ReflectionTestUtils.setField(service, "walkInCustomerId", 1L);
        ReflectionTestUtils.setField(service, "maxOtherAmount", new BigDecimal("500000.00"));
    }

    @Test
    void paidFreeInvoiceCreatesCustomerLinkedOtherChargeAndClosesBooking() {
        User user = new User();
        user.setIdUser(8L);
        when(users.findByUsername("reception")).thenReturn(Optional.of(user));

        when(customers.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setIdCustomer(2L);
            return customer;
        });

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
                java.time.LocalDate.of(2026, 11, 11), 44L, 2L, "Client facturé", null, null, null, null, null,
                null, null, new BigDecimal("120.00"), BigDecimal.ZERO, new BigDecimal("120.00"), null, "{}",
                "PAID", new BigDecimal("120.00"), BigDecimal.ZERO, null, List.of(), List.of(), null);
        when(billingRead.invoiceDetail(77L)).thenReturn(response);

        FreeInvoiceRequest request = new FreeInvoiceRequest(
                new FreeInvoiceRequest.Customer(null, "Jean Client", "0340000000", "Malagasy", "Antananarivo",
                        "Jean", "Client", "jean@example.test", null, null, null, null),
                "Petit-déjeuner", java.time.LocalDate.of(2026, 11, 11), java.time.LocalDate.of(2026, 11, 12),
                List.of(new FreeInvoiceRequest.Line("OTHER", "Petit-déjeuner", new BigDecimal("1.00"), new BigDecimal("120.00"))),
                BigDecimal.ZERO, new FreeInvoiceRequest.Payment("PAID", 3L, null), "Test facture libre", new BigDecimal("120.00"));
        var authentication = new UsernamePasswordAuthenticationToken("reception", "",
                List.of(new SimpleGrantedAuthority("ROLE_Receptionniste")));

        InvoiceDetailResponse result = service.issue(request, authentication);

        assertSame(response, result);
        ArgumentCaptor<Booking> booking = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).saveAndFlush(booking.capture());
        assertEquals(2L, booking.getValue().getCustomer().getIdCustomer());
        ArgumentCaptor<Invoice> invoice = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).saveAndFlush(invoice.capture());
        assertEquals(2L, invoice.getValue().getCustomer().getIdCustomer());
        assertSame(booking.getValue().getCustomer(), invoice.getValue().getCustomer());
        ArgumentCaptor<BookingOther> bookingOther = ArgumentCaptor.forClass(BookingOther.class);
        verify(others).saveAndFlush(bookingOther.capture());
        assertEquals("Petit-déjeuner", bookingOther.getValue().getLabel());
        assertEquals(new BigDecimal("120.00"), bookingOther.getValue().getAmount());
        ArgumentCaptor<InvoiceLine> invoiceLine = ArgumentCaptor.forClass(InvoiceLine.class);
        verify(invoiceLines).save(invoiceLine.capture());
        assertEquals("OTHER", invoiceLine.getValue().getServiceCode());
        assertEquals(new BigDecimal("1.00"), invoiceLine.getValue().getQuantity());
        assertEquals(new BigDecimal("120.00"), invoiceLine.getValue().getUnitPrice());
        verify(payments).saveAndFlush(any());
        verify(statusService).changeBookingStatus(any(Booking.class), eq("CLOSED"), eq("Facture payée intégralement"),
                eq(8L), any());
        verify(bookingHistory).save(any());
    }

    @Test
    void roomLineCannotBeInvoicedWithAQuantityGreaterThanOne() {
        FreeInvoiceRequest request = new FreeInvoiceRequest(
                new FreeInvoiceRequest.Customer(null, "Jean Client", "0340000000", null, null,
                        "Jean", "Client", null, null, null, null, null),
                null, null, null,
                List.of(new FreeInvoiceRequest.Line("ROOM", "Chambre double", new BigDecimal("2.00"), new BigDecimal("120.00"))),
                BigDecimal.ZERO, new FreeInvoiceRequest.Payment("UNPAID", null, null), null, new BigDecimal("240.00"));
        var authentication = new UsernamePasswordAuthenticationToken("reception", "",
                List.of(new SimpleGrantedAuthority("ROLE_Receptionniste")));

        assertThrows(ValidationException.class, () -> service.preview(request, authentication));
    }

    @Test
    void previewAllowsIncompleteNewCustomerWithoutPersistingTheDraft() {
        when(settings.findById(1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(HotelSettings.class)));
        User user = new User();
        user.setIdUser(8L);
        when(users.findByUsername("reception")).thenReturn(Optional.of(user));
        FreeInvoiceRequest request = new FreeInvoiceRequest(
                new FreeInvoiceRequest.Customer(null, "", null, null, null, "", "", null, null, null, null, null),
                null, null, null,
                List.of(new FreeInvoiceRequest.Line("OTHER", "Service", BigDecimal.ONE, new BigDecimal("100.00"))),
                BigDecimal.ZERO, new FreeInvoiceRequest.Payment("UNPAID", null, null), null, new BigDecimal("100.00"));
        var authentication = new UsernamePasswordAuthenticationToken("reception", "",
                List.of(new SimpleGrantedAuthority("ROLE_Receptionniste")));

        InvoiceDetailResponse response = service.preview(request, authentication);

        assertEquals("Client à renseigner", response.billingName());
        assertThrows(ValidationException.class, () -> service.issue(request, authentication));
        verify(customers, never()).saveAndFlush(any());
        verify(bookings, never()).saveAndFlush(any());
    }
}