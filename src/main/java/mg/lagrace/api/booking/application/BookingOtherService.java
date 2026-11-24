package mg.lagrace.api.booking.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import mg.lagrace.api.booking.dto.BookingOtherCreateRequest;
import mg.lagrace.api.booking.dto.BookingOtherUpdateRequest;
import mg.lagrace.api.booking.dto.BookingOtherVoidRequest;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class BookingOtherService {
    @Autowired private BookingPermissionService permissions;
    private final BookingOtherRepository repository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingStatusService statusService;
    private final BookingChangeLogger logger;
    private final BookingDetailService details;
    private final Clock clock;
    @Value("${booking.other.max-amount:0}")
    private BigDecimal maxAmount;

    public BookingOtherService(BookingOtherRepository repository, BookingRepository bookingRepository,
            UserRepository userRepository, BookingStatusService statusService, BookingChangeLogger logger,
            BookingDetailService details, Clock clock) {
        this.repository = repository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.statusService = statusService;
        this.logger = logger;
        this.details = details;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse addToBooking(Long bookingId, BookingOtherCreateRequest request, String username) {
        String permission = request != null && request.amount() != null && request.amount().signum() < 0
                ? "OTHER_DISCOUNT" : "OTHER_ADD";
        permissions.require(username, null, "BOOKING_READ", permission);
        Booking booking = openBooking(bookingId);
        validateNew(request);
        BookingOther other = new BookingOther();
        other.setBooking(booking);
        other.setLabel(request.label().trim());
        other.setAmount(request.amount());
        User user = findUser(username);
        other.setCreatedByUser(user);
        OffsetDateTime now = OffsetDateTime.now(clock);
        other.setCreatedAt(now);
        other.setUpdatedAt(now);
        other = repository.saveAndFlush(other);
        logger.log(bookingId, "OTHER", other.getIdBookingOther(), "CREATE", null, null,
                other.getLabel() + " : " + other.getAmount(), null, user.getIdUser(), UUID.randomUUID());
        return details.get(bookingId);
    }

    @Transactional
    public BookingDetailResponse updateInBooking(Long id, BookingOtherUpdateRequest request, String username) {
        BookingOther other = getById(id);
        boolean discountChange = Boolean.TRUE.equals(other.getIsDiscount())
                || request != null && request.has("amount") && request.getAmount() != null && request.getAmount().signum() < 0;
        permissions.require(username, null, "BOOKING_READ", discountChange ? "OTHER_DISCOUNT" : "OTHER_UPDATE");
        Booking booking = openBooking(other.getBooking().getIdBooking());
        User user = findUser(username);
        if (request == null || request.getVersion() == null || !request.getVersion().equals(other.getVersion()))
            throw new ConflictException("STALE_VERSION", "La demande hors catalogue a été modifiée par quelqu'un d'autre.");
        if (!request.has("label") && !request.has("amount")) throw new ValidationException("Au moins label ou amount est obligatoire.");
        if (other.getVoidedAt() != null) throw new ValidationException("Une demande annulée ne peut plus être modifiée.");
        UUID operationId = UUID.randomUUID();
        if (request.has("label")) {
            String label = cleanLabel(request.getLabel());
            String old = other.getLabel(); other.setLabel(label);
            logger.log(booking.getIdBooking(), "OTHER", id, "UPDATE", "label", old, label, null, user.getIdUser(), operationId);
        }
        if (request.has("amount")) {
            validateAmount(request.getAmount());
            BigDecimal old = other.getAmount(); other.setAmount(request.getAmount());
            logger.log(booking.getIdBooking(), "OTHER", id, "UPDATE", "amount", old, request.getAmount(), null, user.getIdUser(), operationId);
        }
        repository.saveAndFlush(other);
        return details.get(booking.getIdBooking());
    }

    @Transactional
    public BookingDetailResponse voidInBooking(Long id, BookingOtherVoidRequest request, String username) {
        BookingOther other = getById(id);
        permissions.require(username, null, "BOOKING_READ",
                Boolean.TRUE.equals(other.getIsDiscount()) ? "OTHER_DISCOUNT" : "OTHER_VOID");
        Booking booking = openBooking(other.getBooking().getIdBooking());
        User user = findUser(username);
        if (request == null || !request.version().equals(other.getVersion()))
            throw new ConflictException("STALE_VERSION", "La demande hors catalogue a été modifiée par quelqu'un d'autre.");
        if (other.getVoidedAt() != null) throw new ValidationException("Cette demande est déjà annulée.");
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        OffsetDateTime now = OffsetDateTime.now(clock); other.setVoidedAt(now); other.setVoidReason(reason);
        repository.saveAndFlush(other);
        UUID operationId = UUID.randomUUID();
        logger.log(booking.getIdBooking(), "OTHER", id, "VOID", "voided_at", null, now, reason, user.getIdUser(), operationId);
        logger.log(booking.getIdBooking(), "OTHER", id, "VOID", "void_reason", null, reason, reason, user.getIdUser(), operationId);
        return details.get(booking.getIdBooking());
    }

    @Transactional(readOnly = true)
    public List<BookingOther> getAll(Long bookingId) {
        return bookingId == null
                ? repository.findAll()
                : repository.findAllByBooking_IdBookingOrderByCreatedAtDesc(bookingId);
    }

    @Transactional(readOnly = true)
    public List<BookingOther> getByBookingId(Long bookingId) {
        if (bookingId == null) {
            throw new ValidationException("L'identifiant de réservation est obligatoire.");
        }
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Réservation introuvable avec l'id " + bookingId);
        }
        return repository.findAllByBooking_IdBookingOrderByCreatedAtDesc(bookingId);
    }

    @Transactional(readOnly = true)
    public BookingOther getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Autre dépense introuvable avec l'id " + id);
    }

    private Booking openBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id " + id));
        statusService.assertBookingOpen(booking);
        return booking;
    }

    private User findUser(String username) {
        if (username == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private void validateNew(BookingOtherCreateRequest request) {
        if (request == null) throw new ValidationException("Les données sont obligatoires.");
        cleanLabel(request.label());
        validateAmount(request.amount());
    }

    private String cleanLabel(String value) {
        if (value == null || value.isBlank()) throw new ValidationException("Le libellé est obligatoire.");
        String label = value.trim();
        if (label.length() > 150) throw new ValidationException("Le libellé ne peut pas dépasser 150 caractères.");
        return label;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new ValidationException("Le montant doit être strictement positif.");
        if (maxAmount != null && maxAmount.signum() > 0 && amount.compareTo(maxAmount) > 0) {
            throw new ValidationException("Validation d'un responsable requise");
        }
    }
}
