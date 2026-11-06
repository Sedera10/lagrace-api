package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.booking.BookingChargeCreateRequest;
import mg.lagrace.api.dto.booking.BookingChargeUpdateRequest;
import mg.lagrace.api.dto.booking.BookingChargeVoidRequest;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingOtherCreateRequest;
import mg.lagrace.api.dto.booking.BookingOtherUpdateRequest;
import mg.lagrace.api.dto.booking.BookingOtherVoidRequest;
import mg.lagrace.api.dto.booking.BookingUpdateRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomUpdateRequest;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingCharge;
import mg.lagrace.api.models.BookingOther;
import mg.lagrace.api.models.BookingRoom;
import mg.lagrace.api.models.views.BookingBalance;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BookingOtherRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingRoomRepository;

@Service
public class BookingChangesService {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final BookingRepository bookings;
    private final BookingRoomRepository rooms;
    private final BookingChargeRepository charges;
    private final BookingOtherRepository others;
    private final BookingBalanceRepository balances;
    private final BookingUpdateService bookingUpdates;
    private final BookingRoomService roomUpdates;
    private final BookingChargeService chargeUpdates;
    private final BookingOtherService otherUpdates;
    private final BookingDetailService details;
    private final BookingChangeLogger logger;

    public BookingChangesService(BookingRepository bookings, BookingRoomRepository rooms,
            BookingChargeRepository charges, BookingOtherRepository others, BookingBalanceRepository balances,
            BookingUpdateService bookingUpdates, BookingRoomService roomUpdates,
            BookingChargeService chargeUpdates, BookingOtherService otherUpdates, BookingDetailService details,
            BookingChangeLogger logger) {
        this.bookings = bookings; this.rooms = rooms; this.charges = charges; this.others = others;
        this.balances = balances; this.bookingUpdates = bookingUpdates; this.roomUpdates = roomUpdates;
        this.chargeUpdates = chargeUpdates; this.otherUpdates = otherUpdates; this.details = details; this.logger = logger;
    }

    @Transactional
    public BookingDetailResponse apply(Long id, JsonNode request, Authentication authentication) {
            logger.beginOperation();
            try {
        if (request == null || !request.hasNonNull("version"))
            throw new ValidationException("La version du dossier est obligatoire.", "version");
        int version = request.get("version").asInt();
        Booking booking = bookings.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
        if (!Integer.valueOf(version).equals(booking.getVersion()))
            throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");
        if (Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Sur un dossier terminé ou annulé, aucune modification métier n'est possible.");
        if (request.has("status") || request.has("action") || request.has("extend") || request.has("cancel"))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Les changements de statut, prolongements et annulations sont interdits dans cette opération.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");

        validateRooms(id, request.path("rooms"));
        validateCharges(id, request.path("charges"));
        validateOthers(id, request.path("others"));

        JsonNode bookingPatch = request.get("booking");
        if (bookingPatch != null && !bookingPatch.isNull() && bookingPatch.isObject() && bookingPatch.size() > 0) {
            ObjectNode m1 = mapper.createObjectNode();
            m1.put("version", version);
            if (bookingPatch.isObject()) m1.setAll((ObjectNode) bookingPatch);
            bookingUpdates.update(id, mapper.convertValue(m1, BookingUpdateRequest.class), authentication);
        }

        JsonNode roomsNode = request.get("rooms");
        if (roomsNode != null && roomsNode.isArray()) {
            for (int index = 0; index < roomsNode.size(); index++) {
                JsonNode item = roomsNode.get(index);
                try {
                    BookingRoomUpdateRequest roomRequest = new BookingRoomUpdateRequest(
                            item.path("version").asInt(), nullableLong(item, "roomId"), nullableDate(item, "checkIn"),
                            nullableDate(item, "checkOut"), item.path("reason").isMissingNode() ? null : item.path("reason").asText(), null);
                    roomUpdates.updateRoom(item.path("id").asLong(), roomRequest, authentication);
                } catch (BusinessException ex) {
                    throw withField(ex, "rooms[" + index + "]");
                }
            }
        }

        applyCharges(id, request.path("charges"), authentication);
        applyOthers(id, request.path("others"), authentication);

        BookingBalance balance = balances.findById(id)
                .orElseThrow(() -> new IllegalStateException("Solde introuvable dans v_booking_balance."));
        BigDecimal newTotal = n(balance.getTotal());
        if (request.hasNonNull("expectedNewTotal")
                && newTotal.compareTo(request.get("expectedNewTotal").decimalValue()) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total recalculé est différent du total attendu.",
                    "expectedNewTotal", newTotal);
        return details.get(id);
            } finally {
                logger.endOperation();
            }
    }

    private void validateRooms(Long bookingId, JsonNode node) {
        if (!node.isArray()) return;
        for (int index = 0; index < node.size(); index++) {
            JsonNode item = node.get(index);
            if (!item.hasNonNull("id") || !item.hasNonNull("version"))
                throw new ValidationException("L'identifiant et la version de la ligne sont obligatoires.", "rooms[" + index + "]");
            BookingRoom line = rooms.findById(item.get("id").asLong()).orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable."));
            if (!bookingId.equals(line.getBooking().getIdBooking())) throw new ValidationException("La ligne n'appartient pas à ce dossier.", "rooms[" + index + "]");
            if (!Integer.valueOf(item.get("version").asInt()).equals(line.getVersion())) throw new ConflictException("STALE_VERSION", "La ligne chambre a été modifiée.", "rooms[" + index + "]", null);
        }
    }

    private void validateCharges(Long bookingId, JsonNode node) {
        JsonNode updates = node.path("update"); JsonNode voids = node.path("void");
        validateChargeVersions(bookingId, updates, "charges.update"); validateChargeVersions(bookingId, voids, "charges.void");
    }

    private void validateChargeVersions(Long bookingId, JsonNode node, String field) {
        if (!node.isArray()) return;
        for (int index = 0; index < node.size(); index++) {
            JsonNode item = node.get(index);
            BookingCharge charge = charges.findById(item.path("id").asLong()).orElseThrow(() -> new ResourceNotFoundException("Charge introuvable."));
            if (!bookingId.equals(charge.getBooking().getIdBooking())) throw new ValidationException("La charge n'appartient pas à ce dossier.", field + "[" + index + "]");
            if (!item.hasNonNull("version") || !Integer.valueOf(item.get("version").asInt()).equals(charge.getVersion()))
                throw new ConflictException("STALE_VERSION", "La charge a été modifiée.", field + "[" + index + "]", null);
        }
    }

    private void validateOthers(Long bookingId, JsonNode node) {
        validateOtherVersions(bookingId, node.path("update"), "others.update");
        validateOtherVersions(bookingId, node.path("void"), "others.void");
    }

    private void validateOtherVersions(Long bookingId, JsonNode node, String field) {
        if (!node.isArray()) return;
        for (int index = 0; index < node.size(); index++) {
            JsonNode item = node.get(index);
            BookingOther other = others.findById(item.path("id").asLong()).orElseThrow(() -> new ResourceNotFoundException("Demande hors catalogue introuvable."));
            if (!bookingId.equals(other.getBooking().getIdBooking())) throw new ValidationException("La demande n'appartient pas à ce dossier.", field + "[" + index + "]");
            if (!item.hasNonNull("version") || !Integer.valueOf(item.get("version").asInt()).equals(other.getVersion()))
                throw new ConflictException("STALE_VERSION", "La demande hors catalogue a été modifiée.", field + "[" + index + "]", null);
        }
    }

    private void applyCharges(Long id, JsonNode node, Authentication authentication) {
        if (node == null || node.isMissingNode()) return;
        JsonNode add = node.path("add");
        if (add.isArray()) for (JsonNode item : add) chargeUpdates.add(id, mapper.convertValue(item, BookingChargeCreateRequest.class), authentication);
        JsonNode update = node.path("update");
        if (update.isArray()) for (JsonNode item : update) chargeUpdates.update(item.path("id").asLong(), mapper.convertValue(item, BookingChargeUpdateRequest.class), authentication);
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (JsonNode item : voids) chargeUpdates.voidCharge(item.path("id").asLong(), mapper.convertValue(item, BookingChargeVoidRequest.class), authentication);
    }

    private void applyOthers(Long id, JsonNode node, Authentication authentication) {
        if (node == null || node.isMissingNode()) return;
        String username = authentication.getName();
        JsonNode add = node.path("add");
        if (add.isArray()) for (JsonNode item : add) otherUpdates.addToBooking(id, mapper.convertValue(item, BookingOtherCreateRequest.class), username);
        JsonNode update = node.path("update");
        if (update.isArray()) for (JsonNode item : update) otherUpdates.updateInBooking(item.path("id").asLong(), mapper.convertValue(item, BookingOtherUpdateRequest.class), username);
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (JsonNode item : voids) otherUpdates.voidInBooking(item.path("id").asLong(), mapper.convertValue(item, BookingOtherVoidRequest.class), username);
    }

    private BookingDetailResponse applyError(BusinessException ex, String field) { throw withField(ex, field); }
    private RuntimeException withField(BusinessException ex, String field) {
        if (ex instanceof ConflictException) return new ConflictException(ex.getCode(), ex.getMessage(), field, null);
        return new ValidationException(ex.getMessage(), field);
    }
    private Long nullableLong(JsonNode node, String name) { return node.hasNonNull(name) ? node.get(name).asLong() : null; }
    private LocalDate nullableDate(JsonNode node, String name) { return node.hasNonNull(name) ? LocalDate.parse(node.get(name).asText()) : null; }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
