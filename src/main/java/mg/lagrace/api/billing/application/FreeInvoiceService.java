package mg.lagrace.api.billing.application;

import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.customer.IdentificationDocument;
import mg.lagrace.api.customer.persistence.IdentificationDocRepository;
import mg.lagrace.api.reference.Nationality;
import mg.lagrace.api.reference.persistence.NationalityRepository;

import mg.lagrace.api.booking.application.BookingChangeLogger;
import mg.lagrace.api.booking.application.BookingStatusService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingOrigin;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatusHistory;
import mg.lagrace.api.booking.core.BookingStatusHistoryRepository;
import mg.lagrace.api.booking.core.BookingStatusRepository;
import mg.lagrace.api.billing.dto.invoice.FreeInvoiceRequest;
import mg.lagrace.api.billing.dto.invoice.InvoiceDetailResponse;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.billing.Invoice;
import mg.lagrace.api.billing.InvoiceLine;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.payment.Payment;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.customer.persistence.CustomerRepository;
import mg.lagrace.api.reference.system.persistence.HotelSettingsRepository;
import mg.lagrace.api.billing.persistence.InvoiceLineRepository;
import mg.lagrace.api.billing.persistence.InvoiceRepository;
import mg.lagrace.api.billing.persistence.InvoiceStatusRepository;
import mg.lagrace.api.payment.persistence.PaymentKindRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;
import mg.lagrace.api.payment.persistence.PaymentRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class FreeInvoiceService {
    @Autowired private mg.lagrace.api.auth.application.BookingPermissionService permissions;
    private static final Set<String> SERVICE_CODES = Set.of(
            "ROOM", "SPACE", "MEETING_ROOM", "POOL", "RESTAURANT", "EXTRA", "OTHER");

    private final BookingRepository bookings;
    private final BookingStatusRepository bookingStatuses;
    private final BookingStatusHistoryRepository bookingHistory;
    private final BookingOtherRepository others;
    private final CustomerRepository customers;
    private final IdentificationDocRepository documents;
    private final NationalityRepository nationalities;
    private final UserRepository users;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final InvoiceStatusRepository invoiceStatuses;
    private final HotelSettingsRepository settings;
    private final PaymentRepository payments;
    private final PaymentKindRepository paymentKinds;
    private final PaymentMethodRepository paymentMethods;
    private final BookingStatusService statusService;
    private final BookingChangeLogger changeLogger;
    private final BillingReadService billingRead;
    private final Clock clock;

    @Value("${invoice.walk-in-customer-id:1}")
    private Long walkInCustomerId;

    @Value("${booking.other.max-amount:0}")
    private BigDecimal maxOtherAmount;

    public FreeInvoiceService(BookingRepository bookings, BookingStatusRepository bookingStatuses,
            BookingStatusHistoryRepository bookingHistory, BookingOtherRepository others,
            CustomerRepository customers, UserRepository users, InvoiceRepository invoices,
            InvoiceLineRepository invoiceLines, InvoiceStatusRepository invoiceStatuses,
            HotelSettingsRepository settings, PaymentRepository payments,
            PaymentKindRepository paymentKinds, PaymentMethodRepository paymentMethods,
            BookingStatusService statusService, BookingChangeLogger changeLogger,
            BillingReadService billingRead, IdentificationDocRepository documents,
            NationalityRepository nationalities, Clock clock) {
        this.bookings = bookings;
        this.bookingStatuses = bookingStatuses;
        this.bookingHistory = bookingHistory;
        this.others = others;
        this.customers = customers;
        this.documents = documents;
        this.nationalities = nationalities;
        this.users = users;
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.invoiceStatuses = invoiceStatuses;
        this.settings = settings;
        this.payments = payments;
        this.paymentKinds = paymentKinds;
        this.paymentMethods = paymentMethods;
        this.statusService = statusService;
        this.changeLogger = changeLogger;
        this.billingRead = billingRead;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse preview(FreeInvoiceRequest request, Authentication authentication) {
        if (request == null) throw new ValidationException("Les données de la facture sont obligatoires.");
        permissions.require(authentication, "INVOICE_ISSUE");
        boolean manager = permissions.isAuthorized(authentication, "CATALOG_MANAGE");
        boolean canDiscount = permissions.isAuthorized(authentication, "OTHER_DISCOUNT");
        if (request.discount() != null && request.discount().signum() > 0)
            permissions.requireField(authentication, "discount", "OTHER_DISCOUNT");
        if (request.payment() != null && !"UNPAID".equalsIgnoreCase(request.payment().status()))
            permissions.requireField(authentication, "payment", "PAYMENT_COLLECT");
        validateHeader(request, false);
        Customer selectedCustomer = selectedCustomer(request.customer());
        List<BigDecimal> lineAmounts = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (FreeInvoiceRequest.Line line : request.lines()) {
            validateLine(line, manager);
            BigDecimal amount = line.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            lineAmounts.add(amount);
            subtotal = subtotal.add(amount);
        }
        BigDecimal discount = request.discount() == null ? BigDecimal.ZERO : request.discount();
        if (discount.signum() < 0) throw new ValidationException("La remise ne peut pas être négative.", "discount");
        if (discount.signum() > 0 && !canDiscount)
            permissions.requireField(authentication, "discount", "OTHER_DISCOUNT");
        validateOtherAmount(discount, canDiscount, "discount");
        if (discount.compareTo(subtotal) > 0)
            throw new ValidationException("La remise ne peut pas dépasser le sous-total.", "discount");
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (request.expectedTotal() == null || request.expectedTotal().compareTo(total) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total a changé. Vérifiez la facture avant de confirmer.", null, total);
        String paymentStatus = request.payment() == null ? "UNPAID" : request.payment().status();
        validatePayment(request.payment(), paymentStatus == null ? null : paymentStatus.trim().toUpperCase(), total);
        BigDecimal paid = request.payment() == null || "UNPAID".equalsIgnoreCase(request.payment().status())
                ? BigDecimal.ZERO
                : "PAID".equalsIgnoreCase(request.payment().status()) ? total : request.payment().amount();
        List<InvoiceDetailResponse.Line> previewLines = new ArrayList<>();
        int position = 1;
        for (int index = 0; index < request.lines().size(); index++) {
            FreeInvoiceRequest.Line line = request.lines().get(index);
            previewLines.add(new InvoiceDetailResponse.Line(null, line.description().trim(), line.quantity(),
                    line.unitPrice(), lineAmounts.get(index), position++, line.serviceCode().trim().toUpperCase(),
                    "OTHER", null, true, null));
        }
        if (discount.signum() > 0)
            previewLines.add(new InvoiceDetailResponse.Line(null, "Remise libre sur facture", BigDecimal.ONE,
                    discount.negate(), discount.negate(), position, "OTHER", "OTHER", null, true, null));
        return new InvoiceDetailResponse(null, null, "INVOICE", "DRAFT", LocalDate.now(clock), null,
                selectedCustomer == null ? null : selectedCustomer.getIdCustomer(), customerDisplayName(request.customer(), selectedCustomer),
                preferred(request.customer().address(), selectedCustomer == null ? null : selectedCustomer.getAddress()),
                preferred(request.customer().phone(), selectedCustomer == null ? null : selectedCustomer.getPhone()),
                preferred(request.customer().nationality(), nationalityName(selectedCustomer)), null,
                clean(request.referenceLabel()), request.periodStart(), request.periodEnd(), subtotal, discount, total,
                clean(request.notes()), InvoiceIssuanceService.snapshot(settings.findById(1L)
                        .orElseThrow(() -> new ResourceNotFoundException("Paramètres de l'hôtel introuvables."))),
                request.payment() == null ? "UNPAID" : request.payment().status().toUpperCase(), paid,
                total.subtract(paid), null, List.of(), previewLines, null);
    }

    @Transactional
    public InvoiceDetailResponse issue(FreeInvoiceRequest request, Authentication authentication) {
        if (request == null) throw new ValidationException("Les données de la facture sont obligatoires.");
        permissions.require(authentication, "INVOICE_ISSUE");
        boolean manager = permissions.isAuthorized(authentication, "CATALOG_MANAGE");
        boolean canDiscount = permissions.isAuthorized(authentication, "OTHER_DISCOUNT");
        if (request.discount() != null && request.discount().signum() > 0)
            permissions.requireField(authentication, "discount", "OTHER_DISCOUNT");
        if (request.payment() != null && !"UNPAID".equalsIgnoreCase(request.payment().status()))
            permissions.requireField(authentication, "payment", "PAYMENT_COLLECT");

        User user = authenticatedUser(authentication);
        validateHeader(request, true);
        Customer customer = resolveCustomer(request.customer());
        List<BigDecimal> lineAmounts = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (FreeInvoiceRequest.Line line : request.lines()) {
            validateLine(line, manager);
            BigDecimal amount = line.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            lineAmounts.add(amount);
            subtotal = subtotal.add(amount);
        }

        BigDecimal discount = request.discount() == null ? BigDecimal.ZERO : request.discount();
        if (discount.signum() < 0) throw new ValidationException("La remise ne peut pas être négative.", "discount");
        if (discount.signum() > 0 && !canDiscount)
            permissions.requireField(authentication, "discount", "OTHER_DISCOUNT");
        validateOtherAmount(discount, canDiscount, "discount");
        if (discount.compareTo(subtotal) > 0)
            throw new ValidationException("La remise ne peut pas dépasser le sous-total.", "discount");

        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (request.expectedTotal() == null || request.expectedTotal().compareTo(total) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total a changé. Vérifiez la facture avant de confirmer.", null, total);
        String paymentStatus = request.payment() == null ? "UNPAID" : request.payment().status();
        if (paymentStatus == null) throw new ValidationException("Le statut de paiement est obligatoire.", "payment.status");
        paymentStatus = paymentStatus.trim().toUpperCase();
        validatePayment(request.payment(), paymentStatus, total);

        OffsetDateTime now = OffsetDateTime.now(clock);
        UUID operationId = UUID.randomUUID();
        Booking booking = createInvoiceBooking(user, customer, now, operationId);
        List<BookingOther> bookingItems = new ArrayList<>();
        for (int index = 0; index < request.lines().size(); index++) {
            FreeInvoiceRequest.Line line = request.lines().get(index);
            BookingOther other = new BookingOther();
            other.setBooking(booking);
            other.setLabel(line.description().trim());
            other.setAmount(lineAmounts.get(index));
            other.setCreatedByUser(user);
            other.setCreatedAt(now);
            other.setUpdatedAt(now);
                other = others.saveAndFlush(other);
                bookingItems.add(other);
                changeLogger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "CREATE", null,
                    null, other.getLabel() + " : " + other.getAmount(), null, user.getIdUser(), operationId);
        }
        if (discount.signum() > 0) {
            BookingOther other = new BookingOther();
            other.setBooking(booking);
            other.setLabel("Remise libre sur facture");
            other.setAmount(discount.negate());
            other.setIsDiscount(true);
            other.setCreatedByUser(user);
            other.setCreatedAt(now);
            other.setUpdatedAt(now);
                other = others.saveAndFlush(other);
                bookingItems.add(other);
                changeLogger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "CREATE", null,
                    null, other.getLabel() + " : " + other.getAmount(), null, user.getIdUser(), operationId);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceType("INVOICE");
        invoice.setCustomer(customer);
        invoice.setBooking(booking);
        invoice.setStatus(invoiceStatuses.findByCode("ISSUED")
                .orElseThrow(() -> new ResourceNotFoundException("Statut ISSUED introuvable.")));
        invoice.setIssueDate(LocalDate.now(clock));
        invoice.setBillingName(request.customer().name().trim());
        invoice.setBillingAddress(preferred(request.customer().address(), customer.getAddress()));
        invoice.setBillingPhone(preferred(request.customer().phone(), customer.getPhone()));
        invoice.setBillingNationality(preferred(request.customer().nationality(), nationalityName(customer)));
        invoice.setReferenceLabel(clean(request.referenceLabel()));
        invoice.setPeriodStart(request.periodStart());
        invoice.setPeriodEnd(request.periodEnd());
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTotal(total);
        invoice.setNotes(clean(request.notes()));
        invoice.setCreatedByUser(user);
        invoice.setIssuerSnapshot(InvoiceIssuanceService.snapshot(settings.findById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres de l'hôtel introuvables."))));
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);
        invoice = invoices.saveAndFlush(invoice);

        int position = 1;
        for (int index = 0; index < request.lines().size(); index++) {
            FreeInvoiceRequest.Line input = request.lines().get(index);
            createInvoiceLine(invoice, bookingItems.get(index), input.description().trim(), input.serviceCode(),
                    input.quantity(), input.unitPrice(), position++);
        }
        if (discount.signum() > 0) {
            BookingOther discountItem = bookingItems.get(bookingItems.size() - 1);
            createInvoiceLine(invoice, discountItem, discountItem.getLabel(), "OTHER", BigDecimal.ONE,
                    discount.negate(), position);
        }
        invoiceLines.flush();

        if (!"UNPAID".equals(paymentStatus)) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(paymentMethods.findById(request.payment().methodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable.")));
            payment.setKind(paymentKinds.findByCode("PAYMENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Type de paiement PAYMENT introuvable.")));
            payment.setAmount("PAID".equals(paymentStatus) ? total : request.payment().amount());
            payment.setPaidAt(now);
            payment.setReceivedByUser(user);
            payments.saveAndFlush(payment);
        }
        if ("PAID".equals(paymentStatus)) {
            statusService.changeBookingStatus(booking, "CLOSED", "Facture payée intégralement", user.getIdUser(), now);
            changeLogger.log(booking.getIdBooking(), "BOOKING", booking.getIdBooking(), "UPDATE", "status",
                    "IN_PROGRESS", "CLOSED", "Facture payée intégralement", user.getIdUser(), operationId);
        }

        return billingRead.invoiceDetail(invoice.getIdInvoice());
    }

    private Booking createInvoiceBooking(User user, Customer customer, OffsetDateTime now, UUID operationId) {
        var status = bookingStatuses.findByCode("IN_PROGRESS");
        if (status == null) throw new ResourceNotFoundException("Statut IN_PROGRESS introuvable.");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStatus(status);
        booking.setOrigin(BookingOrigin.LOCAL);
        booking.setCreatedByUser(user);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        booking = bookings.saveAndFlush(booking);
        changeLogger.log(booking.getIdBooking(), "BOOKING", booking.getIdBooking(), "CREATE", "customer_id",
            null, customer.getIdCustomer(), null, user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "BOOKING", booking.getIdBooking(), "CREATE", "origin",
            null, BookingOrigin.LOCAL, null, user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "BOOKING", booking.getIdBooking(), "CREATE", "status",
            null, "IN_PROGRESS", null, user.getIdUser(), operationId);

        BookingStatusHistory entry = new BookingStatusHistory();
        entry.setBooking(booking);
        entry.setOldStatusCode(null);
        entry.setNewStatusCode("IN_PROGRESS");
        entry.setChangedByUser(user);
        entry.setChangedAt(now);
        bookingHistory.save(entry);
        return booking;
    }

    private void createInvoiceLine(Invoice invoice, BookingOther source, String description, String serviceCode,
            BigDecimal quantity, BigDecimal unitPrice, int position) {
        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setDescription(description);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setPosition(position);
        line.setServiceCode(serviceCode.trim().toUpperCase());
        line.setSourceType("OTHER");
        line.setSourceId(source.getIdBookingOther());
        line.setBilled(true);
        invoiceLines.save(line);
    }

    private void validateHeader(FreeInvoiceRequest request, boolean requireCustomerDetails) {
        if (request.customer() == null)
            throw new ValidationException("Sélectionnez un client ou renseignez les coordonnées d'un nouveau client.", "customer");
        if (request.lines() == null || request.lines().isEmpty())
            throw new ValidationException("Ajoutez au moins une ligne de service.", "lines");
        if (request.periodStart() != null && request.periodEnd() != null
                && request.periodEnd().isBefore(request.periodStart()))
            throw new ValidationException("La date de fin doit être postérieure ou égale à la date de début.", "periodEnd");
        if (request.referenceLabel() != null && request.referenceLabel().length() > 100)
            throw new ValidationException("La référence ne peut pas dépasser 100 caractères.", "referenceLabel");
        if (!requireCustomerDetails) return;
        if (request.customer().name() == null || request.customer().name().isBlank())
            throw new ValidationException("Le nom du client est obligatoire.", "customer.name");
        if (request.customer().id() == null
                && (request.customer().firstname() == null || request.customer().firstname().isBlank()
                || request.customer().lastname() == null || request.customer().lastname().isBlank()))
            throw new ValidationException("Le nom et le prénom du nouveau client sont obligatoires.", "customer");
        String identityNumber = clean(request.customer().identificationNumber());
        if (request.customer().id() == null
                && ((request.customer().documentId() == null) != (identityNumber == null)))
            throw new ValidationException("Le type de document et le numéro d'identification doivent être fournis ensemble.", "customer.documentId");
    }

    private String customerDisplayName(FreeInvoiceRequest.Customer draft, Customer selectedCustomer) {
        String name = clean(draft.name());
        if (name != null) return name;
        if (selectedCustomer != null) {
            String selectedFirstName = clean(selectedCustomer.getFirstName());
            String selectedLastName = clean(selectedCustomer.getLastName());
            String selectedName = (selectedFirstName == null ? "" : selectedFirstName) + " "
                    + (selectedLastName == null ? "" : selectedLastName);
            if (!selectedName.isBlank()) return selectedName.trim();
        }
        String firstName = clean(draft.firstname());
        String lastName = clean(draft.lastname());
        String draftName = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        return draftName.isBlank() ? "Client à renseigner" : draftName.trim();
    }

    private void validateLine(FreeInvoiceRequest.Line line, boolean manager) {
        if (line == null || line.description() == null || line.description().isBlank())
            throw new ValidationException("La description du service est obligatoire.", "lines.description");
        if (line.description().trim().length() > 150)
            throw new ValidationException("La description ne peut pas dépasser 150 caractères.", "lines.description");
        String code = line.serviceCode() == null ? "" : line.serviceCode().trim().toUpperCase();
        if (!SERVICE_CODES.contains(code)) throw new ValidationException("Code de service invalide.", "lines.serviceCode");
        if (line.quantity() == null || line.quantity().signum() <= 0 || line.quantity().scale() > 2)
            throw new ValidationException("La quantité doit être positive avec au plus deux décimales.", "lines.quantity");
        if ("ROOM".equals(code) && line.quantity().compareTo(BigDecimal.ONE) != 0)
            throw new ValidationException("Une chambre est facturée à l'unité.", "lines.quantity");
        if (line.unitPrice() == null || line.unitPrice().signum() <= 0 || line.unitPrice().scale() > 2)
            throw new ValidationException("Le prix unitaire doit être positif avec au plus deux décimales.", "lines.unitPrice");
        BigDecimal amount = line.quantity().multiply(line.unitPrice()).setScale(2, RoundingMode.HALF_UP);
        validateOtherAmount(amount, manager, "lines.unitPrice");
    }

    private void validateOtherAmount(BigDecimal amount, boolean manager, String field) {
        if (amount.signum() < 0) amount = amount.abs();
        if (maxOtherAmount != null && maxOtherAmount.signum() > 0 && amount.compareTo(maxOtherAmount) > 0 && !manager)
            throw new BusinessException("FORBIDDEN", "La permission CATALOG_MANAGE est requise pour ce montant.", field);
    }

    private void validatePayment(FreeInvoiceRequest.Payment payment, String status, BigDecimal total) {
        if (status == null || !Set.of("PAID", "PARTIAL", "UNPAID").contains(status))
            throw new ValidationException("Statut de paiement invalide.", "payment.status");
        if ("UNPAID".equals(status)) return;
        if (payment == null || payment.methodId() == null)
            throw new ValidationException("Le moyen de paiement est obligatoire.", "payment.methodId");
        if ("PARTIAL".equals(status) && (payment.amount() == null || payment.amount().signum() <= 0
                || payment.amount().compareTo(total) >= 0))
            throw new ValidationException("L'acompte doit être positif et inférieur au total.", "payment.amount");
        if ("PAID".equals(status) && total.signum() <= 0)
            throw new ValidationException("Une facture de total nul ne peut pas être payée.", "payment.status");
    }

    private User authenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null)
            throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String preferred(String value, String fallback) {
        String cleanedValue = clean(value);
        return cleanedValue == null ? clean(fallback) : cleanedValue;
    }

    private String nationalityName(Customer customer) {
        return customer == null || customer.getNationality() == null ? null : customer.getNationality().getName();
    }

    private Customer selectedCustomer(FreeInvoiceRequest.Customer request) {
        if (request.id() == null) return null;
        if (request.id().equals(walkInCustomerId))
            throw new ValidationException("Le client de passage ne peut pas être sélectionné comme client de facture.", "customer.id");
        Customer customer = customers.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec l'id : " + request.id()));
        if (!Boolean.TRUE.equals(customer.getActive()) || customer.getMergedInto() != null)
            throw new ValidationException("Ce client est archivé ou fusionné et ne peut pas être utilisé.", "customer.id");
        return customer;
    }

    private Customer resolveCustomer(FreeInvoiceRequest.Customer request) {
        Customer selected = selectedCustomer(request);
        if (selected != null) return selected;

        String identificationNumber = clean(request.identificationNumber());
        IdentificationDocument document = null;
        if (request.documentId() != null) {
            document = documents.findById(request.documentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type de document introuvable avec l'id : " + request.documentId()));
            Customer existing = customers.findByDocument_IdDocumentAndIdentificationNumber(
                    request.documentId(), identificationNumber).orElse(null);
            if (existing != null) {
                if (!Boolean.TRUE.equals(existing.getActive()) || existing.getMergedInto() != null)
                    throw new ValidationException("La fiche correspondant à cette pièce est archivée ou fusionnée.", "customer.documentId");
                if (existing.getIdCustomer().equals(walkInCustomerId))
                    throw new ValidationException("Le client de passage ne peut pas être sélectionné comme client de facture.", "customer.documentId");
                return existing;
            }
        }

        Customer customer = new Customer();
        customer.setLastName(request.lastname().trim());
        customer.setFirstName(request.firstname().trim());
        customer.setPhone(clean(request.phone()));
        customer.setEmail(clean(request.email()));
        customer.setAddress(clean(request.address()));
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setDocument(document);
        customer.setIdentificationNumber(identificationNumber);
        customer.setActive(true);
        if (request.nationalityId() != null) {
            Nationality nationality = nationalities.findById(request.nationalityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nationalité introuvable avec l'id : " + request.nationalityId()));
            customer.setNationality(nationality);
        }
        return customers.saveAndFlush(customer);
    }

}
