package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingUpdateRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.NewCustomer;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.Customer;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.BookingOther;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingOtherRepository;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.IdentificationDocRepository;
import mg.lagrace.api.repositories.NationalityRepository;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.dto.booking.BookingUpdateRequest.OtherChange;

@Service
public class BookingUpdateService {
    private final BookingRepository bookings;
    private final CustomerRepository customers;
    private final IdentificationDocRepository documents;
    private final NationalityRepository nationalities;
    private final UserRepository users;
    private final BookingChangeLogger logger;
    private final BookingDetailService details;
    private final BookingOtherRepository others;
    private final Clock clock;

    public BookingUpdateService(BookingRepository bookings, CustomerRepository customers,
            IdentificationDocRepository documents, NationalityRepository nationalities, UserRepository users,
            BookingChangeLogger logger, BookingDetailService details, BookingOtherRepository others, Clock clock) {
        this.bookings = bookings; this.customers = customers; this.documents = documents;
        this.nationalities = nationalities; this.users = users;
        this.logger = logger; this.details = details; this.others = others; this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse update(Long id, BookingUpdateRequest request, Authentication authentication) {
        if (request == null || !request.has("version")) throw new ValidationException("La version est obligatoire.");
        if (Set.of("customerId", "newCustomer", "origin", "notes", "others").stream().noneMatch(request::has))
            throw new ValidationException("Au moins un champ doit être modifié.");
        if (request.has("customerId") && request.has("newCustomer"))
            throw new ValidationException("Indiquez customerId ou newCustomer, jamais les deux.");
        Booking booking = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
        if (!request.getVersion().equals(booking.getVersion())) throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");
        boolean terminal = Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode());
        if (terminal && (request.has("customerId") || request.has("newCustomer") || request.has("origin")))
            throw new ConflictException("FORBIDDEN", "Sur un dossier terminé ou annulé, seules les notes sont modifiables.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = users.findByUsername(authentication.getName()).orElse(null);
        Long userId = user == null ? null : user.getIdUser();
        UUID operationId = UUID.randomUUID();

        if (request.has("customerId") || request.has("newCustomer")) {
            Customer old = booking.getCustomer();
            Customer next = request.has("customerId") ? findCustomer(request.getCustomerId()) : createCustomer(request.getNewCustomer());
            booking.setCustomer(next);
            logger.log(id, "BOOKING", id, "UPDATE", "customer_id", old == null ? null : old.getIdCustomer(), next.getIdCustomer(), null, userId, operationId);
        }
        if (request.has("origin")) {
            var old = booking.getOrigin(); booking.setOrigin(request.getOrigin());
            logger.log(id, "BOOKING", id, "UPDATE", "origin", old, request.getOrigin(), null, userId, operationId);
        }
        if (request.has("notes")) {
            String old = booking.getNotes(); booking.setNotes(request.getNotes());
            logger.log(id, "BOOKING", id, "UPDATE", "notes", old, request.getNotes(), null, userId, operationId);
        }
        if (request.has("others")) updateOthers(booking, request.getOthers(), user, operationId);
        bookings.saveAndFlush(booking);
        return details.get(id);
    }

    private Customer findCustomer(Long id) {
        if (id == null) throw new ValidationException("customerId est obligatoire.");
        return customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec l'id : " + id));
    }

    private void updateOthers(Booking booking, java.util.List<OtherChange> changes, User user, UUID operationId) {
        if (changes == null) throw new ValidationException("La liste des demandes hors catalogue est obligatoire.");
        for (OtherChange change : changes) {
            String label = cleanLabel(change.label());
            validateAmount(change.amount());
            if (change.id() == null || change.id() < 0) {
                BookingOther other = new BookingOther();
                other.setBooking(booking); other.setLabel(label); other.setAmount(change.amount());
                other.setCreatedByUser(user);
                OffsetDateTime now = OffsetDateTime.now(clock);
                other.setCreatedAt(now); other.setUpdatedAt(now);
                other = others.saveAndFlush(other);
                logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "CREATE", null, null,
                        label + " : " + change.amount(), null, user.getIdUser(), operationId);
                continue;
            }
            BookingOther other = others.findById(change.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Demande hors catalogue introuvable."));
            if (!booking.getIdBooking().equals(other.getBooking().getIdBooking()))
                throw new ValidationException("La demande hors catalogue n'appartient pas à ce dossier.");
            if (change.version() == null || !change.version().equals(other.getVersion()))
                throw new ConflictException("STALE_VERSION", "La demande hors catalogue a été modifiée par quelqu'un d'autre.");
            if (Boolean.TRUE.equals(change.voided())) {
                if (other.getVoidedAt() == null) {
                    OffsetDateTime now = OffsetDateTime.now(clock);
                    other.setVoidedAt(now); other.setVoidReason(normalizeReason(change.voidReason()));
                    others.saveAndFlush(other);
                    logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "VOID", "voided_at", null, now,
                            other.getVoidReason(), user.getIdUser(), operationId);
                    logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "VOID", "void_reason", null,
                            other.getVoidReason(), other.getVoidReason(), user.getIdUser(), operationId);
                }
                continue;
            }
            if (other.getVoidedAt() != null) throw new ValidationException("Une demande annulée ne peut plus être modifiée.");
            if (!other.getLabel().equals(label)) {
                logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "UPDATE", "label",
                        other.getLabel(), label, null, user.getIdUser(), operationId);
                other.setLabel(label);
            }
            if (other.getAmount().compareTo(change.amount()) != 0) {
                logger.log(booking.getIdBooking(), "OTHER", other.getIdBookingOther(), "UPDATE", "amount",
                        other.getAmount(), change.amount(), null, user.getIdUser(), operationId);
                other.setAmount(change.amount());
            }
            others.saveAndFlush(other);
        }
    }

    private String cleanLabel(String value) {
        if (value == null || value.isBlank()) throw new ValidationException("Le libellé est obligatoire.");
        String label = value.trim();
        if (label.length() > 150) throw new ValidationException("Le libellé ne peut pas dépasser 150 caractères.");
        return label;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new ValidationException("Le montant doit être strictement positif.");
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private Customer createCustomer(NewCustomer request) {
        if (request == null) throw new ValidationException("newCustomer est obligatoire.");
        if (request.lastname() == null || request.lastname().isBlank() || request.firstname() == null || request.firstname().isBlank())
            throw new ValidationException("Le nom et le prénom du client sont obligatoires.");
        if ((request.documentId() == null) != (request.identificationNumber() == null || request.identificationNumber().isBlank()))
            throw new ValidationException("Le type de document et le numéro d'identification doivent être fournis ensemble.");
        Customer customer = new Customer(); customer.setLastName(request.lastname().trim()); customer.setFirstName(request.firstname().trim());
        customer.setDateOfBirth(request.dateOfBirth()); customer.setEmail(request.email()); customer.setPhone(request.phone()); customer.setAddress(request.address());
        customer.setIdentificationNumber(request.identificationNumber());
        if (request.documentId() != null) customer.setDocument(documents.findById(request.documentId()).orElseThrow(() -> new ResourceNotFoundException("Type de document introuvable.")));
        if (request.nationalityId() != null) customer.setNationality(nationalities.findById(request.nationalityId()).orElseThrow(() -> new ResourceNotFoundException("Nationalité introuvable.")));
        return customers.saveAndFlush(customer);
    }
}
