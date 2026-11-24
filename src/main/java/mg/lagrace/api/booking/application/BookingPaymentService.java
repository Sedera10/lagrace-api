package mg.lagrace.api.booking.application;

import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.space.BookingSpace;
import mg.lagrace.api.catalog.space.Space;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingDepositRequest;
import mg.lagrace.api.booking.dto.BookingDepositReturnRequest;
import mg.lagrace.api.booking.dto.BookingPaymentRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.payment.Payment;
import mg.lagrace.api.payment.PaymentKind;
import mg.lagrace.api.payment.PaymentMethod;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.payment.persistence.PaymentKindRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;
import mg.lagrace.api.payment.persistence.PaymentRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class BookingPaymentService {
    @Autowired private BookingPermissionService permissions;
    private static final Set<String> PAYMENT_STATUSES = Set.of("PENDING", "CONFIRMED", "IN_PROGRESS");
        private static final Set<String> REFUND_STATUSES = Set.of("PENDING", "CONFIRMED", "IN_PROGRESS", "CANCELED", "CLOSED");

    private final BookingRepository bookings;
    private final BookingBalanceRepository balances;
    private final BookingChargeRepository charges;
        private final BookingOtherRepository others;
    private final BookingSpaceRepository spaces;
    private final AdditionalServiceRepository additionalServices;
    private final PaymentRepository payments;
    private final PaymentKindRepository kinds;
    private final PaymentMethodRepository methods;
    private final UserRepository users;
    private final BookingStatusService statuses;
    private final BookingDetailService details;
        private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingPaymentService(BookingRepository bookings, BookingBalanceRepository balances,
            BookingChargeRepository charges, BookingOtherRepository others, BookingSpaceRepository spaces,
            AdditionalServiceRepository additionalServices,
            PaymentRepository payments, PaymentKindRepository kinds, PaymentMethodRepository methods,
            UserRepository users, BookingStatusService statuses, BookingDetailService details, Clock clock) {
        this.bookings = bookings;
        this.balances = balances;
        this.charges = charges;
        this.others = others;
        this.spaces = spaces;
        this.additionalServices = additionalServices;
        this.payments = payments;
        this.kinds = kinds;
        this.methods = methods;
        this.users = users;
        this.statuses = statuses;
        this.details = details;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse pay(Long bookingId, BookingPaymentRequest request, Authentication authentication) {
        permissions.require(authentication, "BOOKING_READ",
                request != null && "REFUND".equals(request.kind()) ? "PAYMENT_REFUND" : "PAYMENT_COLLECT");
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + bookingId));
        User user = user(authentication);
        if (request.version() == null || !request.version().equals(booking.getVersion()))
            throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");

        String status = booking.getStatus() == null ? null : booking.getStatus().getCode();
        boolean refund = "REFUND".equals(request.kind());
        if (refund ? !REFUND_STATUSES.contains(status) : !PAYMENT_STATUSES.contains(status))
            throw new ConflictException("FORBIDDEN_TRANSITION", (refund ? "Un remboursement" : "Un paiement")
                    + " est impossible pour un dossier " + (status == null ? "sans statut" : status) + ".");

        BookingBalance balance = balances.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Solde introuvable dans v_booking_balance."));
        BigDecimal availableAmount = refund ? n(balance.getPaid()) : n(balance.getBalance());
        if (request.amount() == null || request.amount().signum() <= 0)
            throw new ConflictException("INVALID_PAYMENT", "Le montant doit être positif.");
        if (availableAmount.signum() <= 0 || request.amount().compareTo(availableAmount) > 0)
            throw new ConflictException("INVALID_PAYMENT", refund
                    ? "Le remboursement ne peut pas dépasser les paiements nets disponibles ("
                            + availableAmount.stripTrailingZeros().toPlainString() + " Ar)."
                    : "Le paiement ne peut pas dépasser le solde restant ("
                            + availableAmount.stripTrailingZeros().toPlainString() + " Ar).");

        PaymentMethod method = methods.findById(request.methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable avec l'id : " + request.methodId()));
        String kindCode = refund ? "REFUND" : "PENDING".equals(status) ? "ADVANCE" : "PAYMENT";
        PaymentKind kind = kinds.findByCode(kindCode)
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement introuvable : " + kindCode));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setKind(kind);
        payment.setAmount(request.amount());
        payment.setReceivedByUser(user);
        payment.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        payments.saveAndFlush(payment);

        if (!refund && "PENDING".equals(status)) {
            statuses.changeBookingStatus(booking, "CONFIRMED", null, user.getIdUser());
            bookings.flush();
        }
        entityManager.refresh(balance);
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse collectSpaceDeposit(Long bookingId, BookingDepositRequest request,
            Authentication authentication) {
        permissions.require(authentication, "BOOKING_READ", "DEPOSIT_COLLECT");
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + bookingId));
        User user = user(authentication);
                if (request == null || request.requestId() == null || request.methodId() == null)
                        throw new mg.lagrace.api.exceptions.ValidationException("Le moyen de paiement et requestId sont obligatoires.");
                BookingDetailResponse replay = idempotentReplay(bookingId, request.requestId(), "DEPOSIT");
                if (replay != null) return replay;
                String bookingStatus = booking.getStatus() == null ? null : booking.getStatus().getCode();
                if (Set.of("CANCELED", "CLOSED").contains(bookingStatus))
            throw new ConflictException("FORBIDDEN_TRANSITION", "La caution ne peut pas être encaissée pour ce dossier.");
        if (spaces.findAllByBooking_IdBooking(bookingId).stream().noneMatch(line -> Boolean.TRUE.equals(line.getActive())))
            throw new ConflictException("INVALID_DEPOSIT", "La caution de l'espace exige une ligne espace active.");

        List<AdditionalService> deposits = additionalServices.findActiveSpaceDeposits();
        if (deposits.size() != 1)
            throw new ConflictException("DEPOSIT_CONFIGURATION", "Un seul service de caution actif doit être configuré pour les espaces.");
        AdditionalService depositService = deposits.get(0);
        boolean alreadyCharged = charges.findAllByBooking_IdBooking(bookingId).stream()
                .anyMatch(charge -> Boolean.TRUE.equals(charge.getDeposit()) && charge.getVoidedAt() == null);
        if (alreadyCharged)
            throw new ConflictException("DEPOSIT_ALREADY_COLLECTED", "Une caution d'espace est déjà encaissée pour ce dossier.");

        PaymentMethod method = methods.findById(request.methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable avec l'id : " + request.methodId()));
        PaymentKind kind = kinds.findByCode("DEPOSIT")
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement DEPOSIT introuvable."));

        BookingCharge charge = new BookingCharge();
        charge.setBooking(booking);
        charge.setAdditionalService(depositService);
        charge.setLabel(depositService.getName());
        charge.setQuantity(1);
        charge.setUnitPrice(depositService.getPrice());
        charge.setIncluded(false);
        charge.setDeposit(true);
        charge.setCreatedByUser(user);
        charges.saveAndFlush(charge);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setKind(kind);
        payment.setAmount(depositService.getPrice());
        payment.setReceivedByUser(user);
        payment.setRequestId(request.requestId());
        payment.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        payments.saveAndFlush(payment);
        balances.findById(bookingId).ifPresent(entityManager::refresh);
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse returnSpaceDeposit(Long bookingId, BookingDepositReturnRequest request,
            Authentication authentication) {
        permissions.require(authentication, "BOOKING_READ", "DEPOSIT_REFUND");
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + bookingId));
        User user = user(authentication);
                if (request == null || request.requestId() == null || request.methodId() == null)
                        throw new mg.lagrace.api.exceptions.ValidationException("Le moyen de paiement et requestId sont obligatoires.");
                BookingDetailResponse replay = idempotentReplay(bookingId, request.requestId(), "DEPOSIT_REFUND");
                if (replay != null) return replay;
        List<mg.lagrace.api.booking.space.BookingSpace> spaceLines = spaces.findAllByBooking_IdBooking(bookingId);
                String bookingStatus = booking.getStatus() == null ? null : booking.getStatus().getCode();
                boolean canceled = "CANCELED".equals(bookingStatus);
                boolean allActiveSpacesEnded = !spaceLines.isEmpty() && spaceLines.stream()
                                .filter(line -> Boolean.TRUE.equals(line.getActive()) || "CHECK_OUT".equals(line.getStatus().getCode()))
                                .allMatch(line -> "CHECK_OUT".equals(line.getStatus().getCode()));
                if (spaceLines.isEmpty() || (!canceled && !allActiveSpacesEnded))
            throw new ConflictException("DEPOSIT_NOT_DUE", "La caution peut être rendue après la fin ou l'annulation de l'événement.");
        BookingBalance balance = balances.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Solde introuvable dans v_booking_balance."));
        BigDecimal held = n(balance.getDepositHeld());
                if (held.signum() <= 0)
                        throw new ConflictException("DEPOSIT_NOT_HELD", "Aucune caution n'est détenue pour ce dossier.");

                BigDecimal withheldAmount = BigDecimal.ZERO;
                String withholdingLabel = null;
                if (request.withholding() != null) {
                        permissions.requireField(authentication, "withholding", "OTHER_DISCOUNT");
                        withholdingLabel = cleanWithholdingLabel(request.withholding().label());
                        withheldAmount = request.withholding().amount();
                        if (withheldAmount == null || withheldAmount.signum() <= 0 || withheldAmount.compareTo(held) > 0)
                                throw new mg.lagrace.api.exceptions.ValidationException("La retenue doit être positive et ne pas dépasser la caution détenue.", "withholding.amount");
                }

        PaymentMethod method = methods.findById(request.methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable avec l'id : " + request.methodId()));
                if (request.withholding() != null) {
                        BookingOther other = new BookingOther();
                        other.setBooking(booking);
                        other.setLabel(withholdingLabel);
                        other.setAmount(withheldAmount);
                        other.setIsCancellationFee(false);
                        other.setCreatedByUser(user);
                        OffsetDateTime now = OffsetDateTime.now(clock);
                        other.setCreatedAt(now);
                        other.setUpdatedAt(now);
                        others.saveAndFlush(other);

                        Payment retained = new Payment();
                        retained.setBooking(booking);
                        retained.setMethod(method);
                        retained.setKind(kinds.findByCode("PAYMENT")
                                        .orElseThrow(() -> new ResourceNotFoundException("Type de paiement PAYMENT introuvable.")));
                        retained.setAmount(withheldAmount);
                        retained.setReceivedByUser(user);
                        retained.setNote("Prélevé sur la caution");
                        payments.saveAndFlush(retained);
                }

                BigDecimal cashToHandBack = held.subtract(withheldAmount);
                PaymentKind kind = kinds.findByCode("DEPOSIT_REFUND")
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement DEPOSIT_REFUND introuvable."));
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setKind(kind);
                payment.setAmount(held);
        payment.setReceivedByUser(user);
                payment.setRequestId(request.requestId());
                payment.setCashToHandBack(cashToHandBack);
        payment.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        payments.saveAndFlush(payment);
                entityManager.refresh(balance);
                return details.get(bookingId).withCashToHandBack(cashToHandBack);
    }

        private BookingDetailResponse idempotentReplay(Long bookingId, UUID requestId, String expectedKind) {
                Payment previous = payments.findByBooking_IdBookingAndRequestId(bookingId, requestId).orElse(null);
                if (previous == null) return null;
                if (previous.getKind() == null || !expectedKind.equals(previous.getKind().getCode()))
                        throw new ConflictException("IDEMPOTENCY_KEY_REUSED", "requestId a déjà été utilisé pour une autre opération.");
                return details.get(bookingId).withCashToHandBack(previous.getCashToHandBack());
        }

        private String cleanWithholdingLabel(String label) {
                if (label == null || label.isBlank())
                        throw new mg.lagrace.api.exceptions.ValidationException("Le libellé de la retenue est obligatoire.", "withholding.label");
                String clean = label.trim();
                if (clean.length() > 150)
                        throw new mg.lagrace.api.exceptions.ValidationException("Le libellé ne peut pas dépasser 150 caractères.", "withholding.label");
                return clean;
        }

    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private User user(Authentication authentication) {
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }
}
