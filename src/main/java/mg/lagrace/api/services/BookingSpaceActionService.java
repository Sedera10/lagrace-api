package mg.lagrace.api.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingStatusActionRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.BookingSpace;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.BookingSpaceRepository;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class BookingSpaceActionService {
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private final BookingSpaceRepository spaces;
    private final BookingBalanceRepository balances;
    private final BookingStatusService statuses;
    private final BookingDetailService details;
    private final UserRepository users;
    private final Clock clock;
    @Value("${deposit.requiredBeforeStart:false}")
    private boolean depositRequiredBeforeStart;

    public BookingSpaceActionService(BookingSpaceRepository spaces, BookingStatusService statuses,
            BookingBalanceRepository balances, BookingDetailService details, UserRepository users, Clock clock) {
        this.spaces = spaces;
        this.statuses = statuses;
        this.balances = balances;
        this.details = details;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse start(Long lineId, BookingStatusActionRequest request,
            Authentication authentication) {
        BookingSpace line = requireLine(lineId, request, authentication);
        String bookingStatus = line.getBooking().getStatus().getCode();
        if (!Set.of("PENDING", "CONFIRMED").contains(bookingStatus))
            throw forbidden("Le début de l'événement est impossible dans l'état actuel du dossier.");
        if (!"CONFIRMED".equals(code(line)))
            throw forbidden("Seule une ligne CONFIRMED peut démarrer l'événement.");
        if (!LocalDate.now(clock.withZone(MADAGASCAR)).equals(line.getEventDate()) && !isAdministrator(authentication))
            throw new ConflictException("FORBIDDEN", "Un début anticipé ou en retard nécessite le rôle Administrateur.");
        if (depositRequiredBeforeStart && balances.findById(line.getBooking().getIdBooking())
                .map(balance -> balance.getDepositHeld() == null || balance.getDepositHeld().signum() <= 0)
                .orElse(true))
            throw forbidden("La caution doit être encaissée avant le début de l'événement.");

        User user = currentUser(authentication);
        OffsetDateTime now = OffsetDateTime.now(clock);
        statuses.changeLineStatus(line, "CHECK_IN", cleanReason(request.reason()), user.getIdUser(), now);
        statuses.changeBookingStatus(line.getBooking(), "IN_PROGRESS", cleanReason(request.reason()), user.getIdUser(), now);
        return details.get(line.getBooking().getIdBooking());
    }

    @Transactional
    public BookingDetailResponse end(Long lineId, BookingStatusActionRequest request,
            Authentication authentication) {
        BookingSpace line = requireLine(lineId, request, authentication);
        if (!"CHECK_IN".equals(code(line)))
            throw forbidden("Seule une ligne CHECK_IN peut terminer l'événement.");
        User user = currentUser(authentication);
        statuses.changeLineStatus(line, "CHECK_OUT", cleanReason(request.reason()), user.getIdUser(), OffsetDateTime.now(clock));
        return details.get(line.getBooking().getIdBooking());
    }

    private BookingSpace requireLine(Long lineId, BookingStatusActionRequest request,
            Authentication authentication) {
        BookingSpace line = spaces.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne espace introuvable avec l'id : " + lineId));
        if (authentication == null)
            throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        if (line.getBooking() == null || line.getBooking().getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(line.getBooking().getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne espace a été modifiée par quelqu'un d'autre.");
        return line;
    }

    private User currentUser(Authentication authentication) {
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private String code(BookingSpace line) {
        return line.getStatus() == null ? null : line.getStatus().getCode();
    }

    private String cleanReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_Administrateur".equals(authority.getAuthority()));
    }

    private ConflictException forbidden(String message) {
        return new ConflictException("FORBIDDEN_TRANSITION", message);
    }
}