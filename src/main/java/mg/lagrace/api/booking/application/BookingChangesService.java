package mg.lagrace.api.booking.application;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;

import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.booking.pool.persistence.BookingPoolRepository;
import mg.lagrace.api.catalog.room.dto.room.RoomRequest;
import mg.lagrace.api.catalog.space.dto.space.SpaceRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.booking.dto.BookingChargeCreateRequest;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.booking.pool.BookingPool;
import mg.lagrace.api.booking.room.BookingRoom;
import mg.lagrace.api.booking.space.BookingSpace;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.additionalservice.CateringCondition;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingBanquetCreateRequest;
import mg.lagrace.api.booking.dto.BookingChargeUpdateRequest;
import mg.lagrace.api.booking.dto.BookingChargeVoidRequest;
import mg.lagrace.api.booking.dto.BookingSpaceUpdateRequest;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.BookingOtherCreateRequest;
import mg.lagrace.api.booking.dto.BookingOtherUpdateRequest;
import mg.lagrace.api.booking.dto.BookingOtherVoidRequest;
import mg.lagrace.api.booking.dto.BookingUpdateRequest;
import mg.lagrace.api.booking.dto.room.BookingRoomUpdateRequest;
import mg.lagrace.api.booking.dto.pool.BookingPoolUpdateRequest;
import mg.lagrace.api.booking.dto.meeting.BookingMeetingRoomUpdateRequest;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.booking.charge.BookingOther;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.booking.charge.persistence.BookingOtherRepository;
import mg.lagrace.api.booking.room.persistence.BookingRoomRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.catalog.space.persistence.BanquetRateRepository;

@Service
public class BookingChangesService {
    @Autowired private BookingPermissionService permissions;
    @PersistenceContext
    private EntityManager entityManager;
    private final ObjectMapper mapper;
    private final BookingRepository bookings;
    private final BookingRoomRepository rooms;
    private final BookingSpaceRepository spaces;
    private final mg.lagrace.api.booking.pool.persistence.BookingPoolRepository pools;
    private final mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository meetingRooms;
    private final BookingChargeRepository charges;
    private final BookingOtherRepository others;
    private final BookingBalanceRepository balances;
    private final BookingUpdateService bookingUpdates;
    private final BookingRoomService roomUpdates;
    private final BookingSpaceService spaceUpdates;
    private final BookingPoolService poolUpdates;
    private final BookingMeetingRoomService meetingRoomUpdates;
    private final BookingChargeService chargeUpdates;
    private final AdditionalServiceRepository additionalServices;
    private final BanquetRateRepository banquetRates;
    private final BookingOtherService otherUpdates;
    private final BookingDetailService details;
    private final BookingChangeLogger logger;

    public BookingChangesService(BookingRepository bookings, BookingRoomRepository rooms,
            BookingSpaceRepository spaces, mg.lagrace.api.booking.pool.persistence.BookingPoolRepository pools, mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository meetingRooms, BookingChargeRepository charges, BookingOtherRepository others, BookingBalanceRepository balances,
            BookingUpdateService bookingUpdates, BookingRoomService roomUpdates, BookingSpaceService spaceUpdates,
            BookingPoolService poolUpdates, BookingMeetingRoomService meetingRoomUpdates, BookingChargeService chargeUpdates, AdditionalServiceRepository additionalServices,
            BanquetRateRepository banquetRates, BookingOtherService otherUpdates, BookingDetailService details,
            BookingChangeLogger logger, ObjectMapper mapper) {
        this.bookings = bookings; this.rooms = rooms; this.spaces = spaces; this.pools = pools; this.meetingRooms = meetingRooms; this.charges = charges; this.others = others;
        this.balances = balances; this.bookingUpdates = bookingUpdates; this.roomUpdates = roomUpdates;
        this.spaceUpdates = spaceUpdates; this.poolUpdates = poolUpdates; this.meetingRoomUpdates = meetingRoomUpdates; this.chargeUpdates = chargeUpdates; this.additionalServices = additionalServices;
        this.banquetRates = banquetRates; this.otherUpdates = otherUpdates; this.details = details; this.logger = logger;
        this.mapper = mapper;
    }

    @Transactional
    public BookingDetailResponse apply(Long id, JsonNode request, Authentication authentication) {
        logger.beginOperation();
        try {
        if (request == null || !request.hasNonNull("version"))
            throw new ValidationException("La version du dossier est obligatoire.", "version");
        int version = request.get("version").asInt();
        Booking booking = bookings.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + id));
        if (!Integer.valueOf(version).equals(booking.getVersion()))
            throw new ConflictException("STALE_VERSION", "Le dossier a été modifié par quelqu'un d'autre.");
        if (Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Sur un dossier terminé ou annulé, aucune modification métier n'est possible.");
        if (request.has("status") || request.has("action") || request.has("extend") || request.has("cancel")
                || request.has("payments") || request.has("deposit") || request.has("cancellation"))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Statuts, paiements, caution et annulations sont interdits dans cette opération.");

        authorizeRequestedChanges(id, request, authentication);
        JsonNode spacesNode = request.path("spaces");
        validateFinalSpaceState(id, spacesNode, request.path("banquet"), request.path("services"));
        validateRooms(id, request.path("rooms"));
        validateTypedLines(id, request.path("pools"), true);
        validateTypedLines(id, request.path("meetingRooms"), false);
        validateCharges(id, request.path("charges"));
        validateCharges(id, request.path("banquet"));
        validateCharges(id, request.path("services"));
        validateOthers(id, request.path("others"));

        JsonNode bookingPatch = request.get("booking");
        if (bookingPatch != null && !bookingPatch.isNull() && bookingPatch.isObject() && bookingPatch.size() > 0) {
            ObjectNode m1 = mapper.createObjectNode();
            m1.put("version", version);
            if (bookingPatch.isObject()) m1.setAll((ObjectNode) bookingPatch);
            bookingUpdates.update(id, mapper.convertValue(m1, BookingUpdateRequest.class), authentication);
        }

        Set<Long> deferredBanquetIds = banquetMutationIds(request.path("banquet"));
        if (spacesNode.isArray()) {
            for (int index = 0; index < spacesNode.size(); index++) {
                JsonNode item = spacesNode.get(index);
                long spaceId = item.path("id").asLong();
                BookingSpaceUpdateRequest spaceRequest = mapper.convertValue(item, BookingSpaceUpdateRequest.class);
                try {
                    spaceUpdates.updateSpaceForAggregate(spaceId, spaceRequest, authentication, deferredBanquetIds);
                } catch (BusinessException ex) {
                    throw withField(ex, "spaces[" + index + "]");
                }
            }
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

        applyPoolChanges(request.path("pools"), authentication);
        applyMeetingRoomChanges(request.path("meetingRooms"), authentication);

            applyBanquet(id, request.path("banquet"), authentication);
            applyServices(id, request.path("services"), authentication);
            applyCharges(id, request.path("charges"), authentication);
        applyOthers(id, request.path("others"), authentication);

        BookingBalance balance = balances.findById(id)
                .orElseThrow(() -> new IllegalStateException("Solde introuvable dans v_booking_balance."));
        entityManager.refresh(balance);
        BigDecimal newTotal = n(balance.getTotal());
        if (request.hasNonNull("expectedNewTotal")
                && newTotal.compareTo(request.get("expectedNewTotal").decimalValue()) != 0)
                throw new ConflictException("IMPACT_CONFIRMATION_REQUIRED",
                    "Les modifications changent le total de " + request.get("expectedNewTotal").decimalValue()
                        + " à " + newTotal + " Ar. Confirmez leur impact avant de continuer.",
                    "expectedNewTotal", newTotal);
        return details.get(id);
        } finally {
            logger.endOperation();
        }
    }

    /** Preflights every M8 permission before the first delegated service can mutate the aggregate. */
    private void authorizeRequestedChanges(Long bookingId, JsonNode request, Authentication authentication) {
        permissions.require(authentication, "BOOKING_READ");
        JsonNode bookingPatch = request.path("booking");
        if (bookingPatch.isObject() && !bookingPatch.isEmpty()) permissions.requireField(authentication, "booking", "BOOKING_UPDATE");

        JsonNode roomsNode = request.path("rooms");
        if (roomsNode.isArray()) for (int i = 0; i < roomsNode.size(); i++) {
            JsonNode item = roomsNode.get(i);
            String field = "rooms[" + i + "]";
            String changedField = List.of("roomId", "checkIn", "checkOut").stream().filter(item::has)
                    .findFirst().map(property -> field + "." + property).orElse(field);
            permissions.requireField(authentication, changedField, "ROOM_UPDATE");
        }

        JsonNode spacesNode = request.path("spaces");
        if (spacesNode.isArray()) for (int i = 0; i < spacesNode.size(); i++) {
            JsonNode item = spacesNode.get(i);
            String field = "spaces[" + i + "]";
            permissions.requireField(authentication, field, "BOOKING_UPDATE");
            if (item.path("shiftLinked").isArray() && !item.path("shiftLinked").isEmpty())
                permissions.requireField(authentication, field + ".shiftLinked", "ROOM_UPDATE");
            if (item.has("withCaterer")) {
                BookingSpace current = spaces.findById(item.path("id").asLong()).orElse(null);
                if (current != null && !java.util.Objects.equals(current.getWithCaterer(), item.path("withCaterer").booleanValue()))
                    permissions.requireField(authentication, field + ".withCaterer", "CHARGE_SET_INCLUDED");
            }
        }
        if (hasArrayEntries(request.path("pools"))) permissions.requireField(authentication, "pools", "BOOKING_UPDATE");
        if (hasArrayEntries(request.path("meetingRooms"))) permissions.requireField(authentication, "meetingRooms", "BOOKING_UPDATE");

        for (String group : List.of("charges", "services", "banquet"))
            authorizeChargeGroup(request.path(group), group, authentication);
        authorizeOtherGroup(request.path("others"), authentication);
    }

    private void authorizeChargeGroup(JsonNode node, String group, Authentication authentication) {
        if (node == null || !node.isObject()) return;
        for (String action : List.of("add", "addBanquet")) {
            JsonNode items = node.path(action);
            if (!items.isArray()) continue;
            for (int i = 0; i < items.size(); i++) {
                JsonNode item = items.get(i);
                String field = group + "." + action + "[" + i + "]";
                permissions.requireField(authentication, field, "CHARGE_ADD");
                if (item.path("included").asBoolean(false)) permissions.requireField(authentication, field + ".included", "CHARGE_SET_INCLUDED");
            }
        }
        JsonNode updates = node.path("update");
        if (updates.isArray()) for (int i = 0; i < updates.size(); i++) {
            JsonNode item = updates.get(i);
            String field = group + ".update[" + i + "]";
            permissions.requireField(authentication, field, "CHARGE_UPDATE");
            if (item.has("included")) {
                BookingCharge charge = charges.findById(item.path("id").asLong()).orElse(null);
                if (charge == null || !java.util.Objects.equals(charge.getIncluded(), item.path("included").booleanValue()))
                    permissions.requireField(authentication, field + ".included", "CHARGE_SET_INCLUDED");
            }
            if ((item.has("quantity") && item.path("quantity").asInt() == 0)
                    || (item.has("persons") && item.path("persons").asInt() == 0))
                permissions.requireField(authentication, field, "CHARGE_VOID");
        }
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (int i = 0; i < voids.size(); i++)
            permissions.requireField(authentication, group + ".void[" + i + "]", "CHARGE_VOID");
    }

    private void authorizeOtherGroup(JsonNode node, Authentication authentication) {
        if (node == null || !node.isObject()) return;
        JsonNode adds = node.path("add");
        if (adds.isArray()) for (int i = 0; i < adds.size(); i++) {
            JsonNode item = adds.get(i);
            String permission = item.path("amount").decimalValue().signum() < 0 ? "OTHER_DISCOUNT" : "OTHER_ADD";
            permissions.requireField(authentication, "others.add[" + i + "]", permission);
        }
        JsonNode updates = node.path("update");
        if (updates.isArray()) for (int i = 0; i < updates.size(); i++) {
            JsonNode item = updates.get(i);
            String field = "others.update[" + i + "]";
            BookingOther other = others.findById(item.path("id").asLong()).orElse(null);
            boolean discount = other != null && Boolean.TRUE.equals(other.getIsDiscount())
                    || item.has("amount") && item.path("amount").decimalValue().signum() < 0;
            permissions.requireField(authentication, field, discount ? "OTHER_DISCOUNT" : "OTHER_UPDATE");
        }
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (int i = 0; i < voids.size(); i++) {
            JsonNode item = voids.get(i);
            BookingOther other = others.findById(item.path("id").asLong()).orElse(null);
            String permission = other != null && Boolean.TRUE.equals(other.getIsDiscount()) ? "OTHER_DISCOUNT" : "OTHER_VOID";
            permissions.requireField(authentication, "others.void[" + i + "]", permission);
        }
    }

    private boolean hasArrayEntries(JsonNode node) { return node != null && node.isArray() && !node.isEmpty(); }

    private void validateTypedLines(Long bookingId, JsonNode node, boolean pool) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (!node.isArray()) throw new ValidationException(pool ? "pools doit être une liste." : "meetingRooms doit être une liste.", pool ? "pools" : "meetingRooms");
        for (int index = 0; index < node.size(); index++) {
            JsonNode item = node.get(index); String field = (pool ? "pools[" : "meetingRooms[") + index + "]";
            if (!item.hasNonNull("id") || !item.hasNonNull("version")) throw new ValidationException("L'identifiant et la version sont obligatoires.", field);
            if (pool) {
                BookingPool line = pools.findById(item.path("id").asLong()).orElseThrow(() -> new ResourceNotFoundException("Ligne piscine introuvable."));
                if (!bookingId.equals(line.getBooking().getIdBooking())) throw new ValidationException("La ligne piscine n'appartient pas à ce dossier.", field);
                if (!Integer.valueOf(item.path("version").asInt()).equals(line.getVersion())) throw new ConflictException("STALE_VERSION", "La ligne piscine a été modifiée.", field, null);
            } else {
                BookingMeetingRoom line = meetingRooms.findById(item.path("id").asLong()).orElseThrow(() -> new ResourceNotFoundException("Ligne salle introuvable."));
                if (!bookingId.equals(line.getBooking().getIdBooking())) throw new ValidationException("La ligne salle n'appartient pas à ce dossier.", field);
                if (!Integer.valueOf(item.path("version").asInt()).equals(line.getVersion())) throw new ConflictException("STALE_VERSION", "La ligne salle a été modifiée.", field, null);
            }
        }
    }

    private void applyPoolChanges(JsonNode node, Authentication authentication) {
        if (node == null || !node.isArray()) return;
        for (int index = 0; index < node.size(); index++) try {
            JsonNode item = node.get(index); ObjectNode patch = ((ObjectNode) item).deepCopy(); patch.remove("id");
            poolUpdates.update(item.path("id").asLong(), mapper.convertValue(patch, BookingPoolUpdateRequest.class), authentication);
        } catch (BusinessException ex) { throw withField(ex, "pools[" + index + "]"); }
    }

    private void applyMeetingRoomChanges(JsonNode node, Authentication authentication) {
        if (node == null || !node.isArray()) return;
        for (int index = 0; index < node.size(); index++) try {
            JsonNode item = node.get(index); ObjectNode patch = ((ObjectNode) item).deepCopy(); patch.remove("id");
            meetingRoomUpdates.update(item.path("id").asLong(), mapper.convertValue(patch, BookingMeetingRoomUpdateRequest.class), authentication);
        } catch (BusinessException ex) { throw withField(ex, "meetingRooms[" + index + "]"); }
    }

    private void validateFinalSpaceState(Long bookingId, JsonNode spacesNode, JsonNode banquetNode,
            JsonNode servicesNode) {
        Map<Long, BookingSpaceUpdateRequest> spaceUpdatesById = new HashMap<>();
        if (spacesNode.isArray()) {
            for (int index = 0; index < spacesNode.size(); index++) {
                JsonNode item = spacesNode.get(index);
                if (!item.hasNonNull("id") || !item.hasNonNull("version"))
                    throw new ValidationException("L'identifiant et la version de la ligne espace sont obligatoires.", "spaces[" + index + "]");
                Long lineId = item.path("id").asLong();
                BookingSpace line = spaces.findById(lineId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ligne espace introuvable."));
                if (line.getBooking() == null || !bookingId.equals(line.getBooking().getIdBooking()))
                    throw new ValidationException("La ligne espace n'appartient pas à ce dossier.", "spaces[" + index + "]");
                ObjectNode patch = ((ObjectNode) item).deepCopy();
                patch.remove("id");
                BookingSpaceUpdateRequest spaceRequest = mapper.convertValue(patch, BookingSpaceUpdateRequest.class);
                if (spaceUpdatesById.put(lineId, spaceRequest) != null)
                    throw new ValidationException("Une ligne espace ne peut apparaître qu'une fois.", "spaces[" + index + "]");
                try {
                    spaceUpdates.validateAggregateSpaceUpdate(lineId, spaceRequest);
                } catch (BusinessException ex) {
                    throw withField(ex, "spaces[" + index + "]");
                }
            }
        } else if (spacesNode != null && !spacesNode.isMissingNode() && !spacesNode.isNull()) {
            throw new ValidationException("spaces doit être une liste.", "spaces");
        }

        List<FinalSpace> finalSpaces = new ArrayList<>();
        for (BookingSpace line : spaces.findAllByBooking_IdBooking(bookingId)) {
            if (!Boolean.TRUE.equals(line.getActive())) continue;
            BookingSpaceUpdateRequest patch = spaceUpdatesById.get(line.getIdBookingSpace());
            Long spaceId = patch == null || patch.spaceId() == null ? line.getSpace().getIdSpace() : patch.spaceId();
            LocalDate eventDate = patch == null || patch.eventDate() == null ? line.getEventDate() : patch.eventDate();
            Integer guests = patch == null || patch.guestsCount() == null ? line.getGuestsCount() : patch.guestsCount();
            Boolean withCaterer = patch == null || patch.withCaterer() == null ? line.getWithCaterer() : patch.withCaterer();
            finalSpaces.add(new FinalSpace(line.getIdBookingSpace(), spaceId, eventDate, guests, Boolean.TRUE.equals(withCaterer)));
        }
        boolean withCaterer = finalSpaces.stream().anyMatch(FinalSpace::withCaterer);
        FinalSpace cateredSpace = finalSpaces.stream().filter(FinalSpace::withCaterer).findFirst()
                .orElse(finalSpaces.stream().findFirst().orElse(null));

        validateChargeVersions(bookingId, banquetNode, "banquet");
        validateChargeVersions(bookingId, servicesNode, "services");
        Set<Long> banquetVoids = ids(banquetNode.path("void"));
        Map<Long, Integer> banquetPersonUpdates = new HashMap<>();
        JsonNode banquetUpdates = banquetNode.path("update");
        if (banquetUpdates.isArray()) for (int index = 0; index < banquetUpdates.size(); index++) {
            JsonNode item = banquetUpdates.get(index);
            if (!item.hasNonNull("persons") && !item.hasNonNull("quantity"))
                throw new ValidationException("Le nombre de personnes est obligatoire.", "banquet.update[" + index + "].persons");
            int persons = item.hasNonNull("persons") ? item.path("persons").asInt() : item.path("quantity").asInt();
            if (persons < 1) throw new ValidationException("Le nombre de personnes doit être positif.", "banquet.update[" + index + "].persons");
            banquetPersonUpdates.put(item.path("id").asLong(), persons);
        }

        List<FinalMenu> finalMenus = new ArrayList<>();
        Set<Long> menuTypeIds = new HashSet<>();
        for (BookingCharge charge : charges.findAllByBooking_IdBooking(bookingId)) {
            if (charge.getVoidedAt() != null || charge.getBanquetRate() == null
                    || banquetVoids.contains(charge.getIdBookingCharge())) continue;
            int persons = banquetPersonUpdates.getOrDefault(charge.getIdBookingCharge(), charge.getQuantity());
            Long menuTypeId = charge.getBanquetRate().getMenuType().getIdMenuType();
            addFinalMenu(finalMenus, menuTypeIds, menuTypeId, persons, "banquet.update");
        }
        JsonNode banquetAdds = banquetNode.path("add");
        if (banquetAdds.isMissingNode()) banquetAdds = banquetNode.path("addBanquet");
        if (banquetAdds.isArray()) for (int index = 0; index < banquetAdds.size(); index++) {
            JsonNode item = banquetAdds.get(index);
            Long menuTypeId = nullableLong(item, "menuTypeId");
            int persons = item.hasNonNull("persons") ? item.path("persons").asInt() : item.path("quantity").asInt();
            if (menuTypeId == null || persons < 1)
                throw new ValidationException("Le type de menu et un nombre de personnes positif sont obligatoires.", "banquet.add[" + index + "]");
            addFinalMenu(finalMenus, menuTypeIds, menuTypeId, persons, "banquet.add[" + index + "]");
        }
        if (withCaterer && finalMenus.isEmpty())
            throw new ValidationException("Avec traiteur, au moins un menu banquet actif est obligatoire.", "banquet");
        if (!withCaterer && !finalMenus.isEmpty())
            throw new ValidationException("Retirez les menus banquet ou gardez le mode traiteur activé.", "banquet");
        for (int index = 0; index < finalMenus.size(); index++) {
            FinalMenu menu = finalMenus.get(index);
            if (cateredSpace == null || menu.persons() > cateredSpace.guestsCount())
                throw new ValidationException("Le nombre de personnes du menu ne peut pas dépasser le nombre d'invités.", "banquet[" + index + "].persons");
            long applicableRates = banquetRates.findApplicableByMenuType(menu.menuTypeId(), cateredSpace.eventDate()).stream()
                    .filter(rate -> menu.persons() >= rate.getMinPersons()
                            && (rate.getMaxPersons() == null || menu.persons() <= rate.getMaxPersons())).count();
            if (applicableRates != 1)
                throw new ValidationException("Aucun palier de banquet applicable à cette date et pour ce nombre de personnes.", "banquet[" + index + "]");
        }

        Set<Long> serviceVoids = ids(servicesNode.path("void"));
        List<AdditionalService> finalSpaceServices = new ArrayList<>();
        for (BookingCharge charge : charges.findAllByBooking_IdBooking(bookingId)) {
            if (charge.getVoidedAt() != null || charge.getAdditionalService() == null
                    || serviceVoids.contains(charge.getIdBookingCharge())) continue;
            String scope = charge.getAdditionalService().getScope().getCode();
            if ("SPACE".equalsIgnoreCase(scope) || "BANQUET".equalsIgnoreCase(scope))
                finalSpaceServices.add(charge.getAdditionalService());
        }
        JsonNode serviceAdds = servicesNode.path("add");
        if (serviceAdds.isArray()) for (int index = 0; index < serviceAdds.size(); index++) {
            JsonNode item = serviceAdds.get(index);
            if (!item.hasNonNull("additionalServiceId")) continue;
            AdditionalService service = additionalServices.findById(item.path("additionalServiceId").asLong())
                    .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
            String scope = service.getScope() == null ? "" : service.getScope().getCode();
            boolean matchingLine = switch (scope.toUpperCase()) {
                case "SPACE" -> !finalSpaces.isEmpty();
                case "BANQUET" -> withCaterer;
                default -> false;
            };
            if (!matchingLine || service.isDeposit())
                throw new ValidationException("Le service ne correspond pas à une ligne espace active.", "services.add[" + index + "]");
            finalSpaceServices.add(service);
        }
        for (int index = 0; index < finalSpaceServices.size(); index++) {
            CateringCondition condition = finalSpaceServices.get(index).getCateringCondition();
            if ((withCaterer && condition == CateringCondition.WITHOUT_CATERER)
                    || (!withCaterer && condition == CateringCondition.WITH_CATERER))
                throw new ValidationException("Le service " + finalSpaceServices.get(index).getName()
                        + " est incompatible avec le mode traiteur final.", "services[" + index + "]");
        }
    }

    private void addFinalMenu(List<FinalMenu> menus, Set<Long> menuTypeIds, Long menuTypeId, int persons, String field) {
        if (!menuTypeIds.add(menuTypeId))
            throw new ValidationException("Un type de menu banquet ne peut apparaître qu'une fois.", field + ".menuTypeId");
        menus.add(new FinalMenu(menuTypeId, persons));
    }

    private Set<Long> ids(JsonNode node) {
        Set<Long> ids = new HashSet<>();
        if (node != null && node.isArray()) for (JsonNode item : node) ids.add(item.path("id").asLong());
        return ids;
    }

    private Set<Long> banquetMutationIds(JsonNode node) {
        Set<Long> ids = new HashSet<>();
        for (String action : List.of("update", "void")) {
            JsonNode items = node.path(action);
            if (items.isArray()) for (JsonNode item : items) if (item.hasNonNull("id")) ids.add(item.path("id").asLong());
        }
        return ids;
    }

    private record FinalSpace(Long id, Long spaceId, LocalDate eventDate, Integer guestsCount, boolean withCaterer) {}
    private record FinalMenu(Long menuTypeId, int persons) {}

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
        JsonNode addBanquet = node.path("addBanquet");
        if (addBanquet.isArray()) for (JsonNode item : addBanquet)
            chargeUpdates.addBanquet(id, mapper.convertValue(item, BookingBanquetCreateRequest.class), authentication);
        JsonNode update = node.path("update");
        if (update.isArray()) for (JsonNode item : update) chargeUpdates.update(item.path("id").asLong(), mapper.convertValue(item, BookingChargeUpdateRequest.class), authentication);
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (JsonNode item : voids) chargeUpdates.voidCharge(item.path("id").asLong(), mapper.convertValue(item, BookingChargeVoidRequest.class), authentication);
    }

    private void applyBanquet(Long id, JsonNode node, Authentication authentication) {
        if (node == null || node.isMissingNode()) return;
        JsonNode add = node.path("add");
        if (add.isMissingNode()) add = node.path("addBanquet");
        if (add.isArray()) for (JsonNode item : add)
            chargeUpdates.addBanquet(id, mapper.convertValue(item, BookingBanquetCreateRequest.class), authentication);
        JsonNode update = node.path("update");
        if (update.isArray()) for (JsonNode item : update)
            chargeUpdates.update(item.path("id").asLong(), mapper.convertValue(item, BookingChargeUpdateRequest.class), authentication);
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (JsonNode item : voids)
            chargeUpdates.voidCharge(item.path("id").asLong(), mapper.convertValue(item, BookingChargeVoidRequest.class), authentication);
    }

    private void applyServices(Long id, JsonNode node, Authentication authentication) {
        if (node == null || node.isMissingNode()) return;
        JsonNode add = node.path("add");
        if (add.isArray()) for (JsonNode item : add)
            chargeUpdates.add(id, mapper.convertValue(item, BookingChargeCreateRequest.class), authentication);
        JsonNode update = node.path("update");
        if (update.isArray()) for (JsonNode item : update)
            chargeUpdates.update(item.path("id").asLong(), mapper.convertValue(item, BookingChargeUpdateRequest.class), authentication);
        JsonNode voids = node.path("void");
        if (voids.isArray()) for (JsonNode item : voids)
            chargeUpdates.voidCharge(item.path("id").asLong(), mapper.convertValue(item, BookingChargeVoidRequest.class), authentication);
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
