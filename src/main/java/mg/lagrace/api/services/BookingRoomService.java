package mg.lagrace.api.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
// import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import mg.lagrace.api.dto.booking.room.BookingLineResponse;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomResponse;
import mg.lagrace.api.dto.booking.room.BookingResponse;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.RoomItem;
import mg.lagrace.api.dto.booking.room.BookingRoomUpdateRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomExtendRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomCheckInRequest;
import mg.lagrace.api.dto.booking.room.BookingRoomCheckOutRequest;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingCharge;
import mg.lagrace.api.models.BookingLineStatus;
import mg.lagrace.api.models.BookingRoom;
import mg.lagrace.api.models.BookingStatus;
import mg.lagrace.api.models.BookingStatusHistory;
import mg.lagrace.api.models.Customer;
import mg.lagrace.api.models.IdentificationDocument;
import mg.lagrace.api.models.MenuItem;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.models.Nationality;
import mg.lagrace.api.models.Payment;
import mg.lagrace.api.models.PaymentKind;
import mg.lagrace.api.models.PaymentMethod;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.views.BookingBalance;
import mg.lagrace.api.models.views.BookingLineView;
import mg.lagrace.api.models.enums.BookingOrigin;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.BookingLineViewRepository;
import mg.lagrace.api.repositories.BookingLineStatusRepository;
import mg.lagrace.api.repositories.BookingRoomRepository;
import mg.lagrace.api.repositories.BookingStatusRepository;
import mg.lagrace.api.repositories.BookingStatusHistoryRepository;
import mg.lagrace.api.repositories.CustomerRepository;
import mg.lagrace.api.repositories.IdentificationDocRepository;
import mg.lagrace.api.repositories.MenuItemRateRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import mg.lagrace.api.repositories.NationalityRepository;
import mg.lagrace.api.repositories.PaymentKindRepository;
import mg.lagrace.api.repositories.PaymentMethodRepository;
import mg.lagrace.api.repositories.PaymentRepository;
import mg.lagrace.api.repositories.RoomRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class BookingRoomService {
    private final BookingRepository bookingRepo;
    private final BookingRoomRepository bookingRoomRepo;
    private final BookingChargeRepository bookingChargeRepo;
    private final BookingBalanceRepository bookingBalanceRepo;
    private final BookingLineViewRepository bookingLineViewRepo;
    private final BookingStatusHistoryRepository historyRepo;
    private final BookingLineStatusRepository lineStatusRepo;
    private final CustomerRepository customerRepository;
    private final IdentificationDocRepository docRepository;
    private final NationalityRepository nationalityRepo;
    private final UserRepository userRepo;
    private final BookingStatusRepository statusRepo;
    private final RoomRepository roomRepo;
    private final RoomTypeService roomTypeService;
    private final AdditionalServiceRepository additionalServiceRepo;
    private final MenuItemRepository menuItemRepo;
    private final MenuItemRateRepository menuItemRateRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentKindRepository paymentKindRepo;
    private final PaymentMethodRepository paymentMethodRepo;
    private final BookingStatusService bookingStatusService;
    private final BookingChangeLogger changeLogger;
    private final BookingDetailService bookingDetailService;
    private final Clock clock;

    public BookingRoomService(BookingRepository bookingRepo, BookingRoomRepository bookingRoomRepo,
            BookingChargeRepository bookingChargeRepo, BookingBalanceRepository bookingBalanceRepo,
            BookingLineViewRepository bookingLineViewRepo,
            BookingStatusHistoryRepository historyRepo,
            BookingLineStatusRepository lineStatusRepo,
            CustomerRepository customerRepository,IdentificationDocRepository docRepository,
            NationalityRepository nationalityRepo,RoomRepository roomRepo,
            UserRepository userRepo, BookingStatusRepository statusRepo,
            RoomTypeService roomTypeService,
            AdditionalServiceRepository additionalServiceRepo,
            MenuItemRepository menuItemRepo, MenuItemRateRepository menuItemRateRepo,
            PaymentRepository paymentRepo, PaymentKindRepository paymentKindRepo,
            PaymentMethodRepository paymentMethodRepo, BookingStatusService bookingStatusService,
            BookingChangeLogger changeLogger, BookingDetailService bookingDetailService, Clock clock) {

        this.bookingRepo = bookingRepo;
        this.bookingRoomRepo = bookingRoomRepo;
        this.bookingChargeRepo = bookingChargeRepo;
        this.bookingBalanceRepo = bookingBalanceRepo;
        this.bookingLineViewRepo = bookingLineViewRepo;
        this.historyRepo = historyRepo;
        this.lineStatusRepo = lineStatusRepo;
        this.customerRepository = customerRepository;
        this.docRepository = docRepository;
        this.nationalityRepo = nationalityRepo;
        this.userRepo = userRepo;
        this.statusRepo = statusRepo;
        this.roomRepo = roomRepo;
        this.roomTypeService = roomTypeService;
        this.additionalServiceRepo = additionalServiceRepo;
        this.menuItemRepo = menuItemRepo;
        this.menuItemRateRepo = menuItemRateRepo;
        this.paymentRepo = paymentRepo;
        this.paymentKindRepo = paymentKindRepo;
        this.paymentMethodRepo = paymentMethodRepo;
        this.bookingStatusService = bookingStatusService;
        this.changeLogger = changeLogger;
        this.bookingDetailService = bookingDetailService;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse updateRoom(Long id, BookingRoomUpdateRequest request,
            Authentication authentication) {
        BookingRoom line = bookingRoomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode())) {
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        }
        if (request == null || request.version() == null || !request.version().equals(line.getVersion())) {
            throw new ConflictException("STALE_VERSION", "La ligne chambre a été modifiée par quelqu'un d'autre.");
        }
        String status = line.getStatus() == null ? "" : line.getStatus().getCode();
        if ("CHECK_IN".equals(status)) {
            throw new ConflictException("FORBIDDEN_TRANSITION", "Utilisez Prolonger pour modifier une ligne déjà enregistrée à l'arrivée.");
        }
        if (!"CONFIRMED".equals(status)) {
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être modifiée.");
        }
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        LocalDate checkIn = request.checkIn() == null ? line.getCheckIn() : request.checkIn();
        LocalDate checkOut = request.checkOut() == null ? line.getCheckOut() : request.checkOut();
        if (checkOut == null || checkIn == null || !checkOut.isAfter(checkIn))
            throw new ValidationException("La date de départ doit être après la date d'arrivée.");
        if (checkIn.isBefore(LocalDate.now(clock)))
            throw new ValidationException("La date d'arrivée ne peut pas être dans le passé.");
        if (request.reason() != null && request.reason().trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.");

        Long roomId = request.roomId() == null ? line.getRoom().getIdRoom() : request.roomId();
        Room room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id : " + roomId));
        assertRoomReservable(room);
        if (!Boolean.TRUE.equals(bookingRoomRepo.isDispo(roomId, checkIn, checkOut, id)))
            throw new ConflictException("ROOM_UNAVAILABLE", "La chambre n'est pas disponible pour cette période.");

        boolean pricingChanged = !room.getType().getIdType().equals(line.getRoom().getType().getIdType())
                || !checkIn.equals(line.getCheckIn());
        BigDecimal unitPrice = pricingChanged ? findRoomNightRate(room, checkIn) : line.getUnitPrice();
        UUID operationId = UUID.randomUUID();
        changeLogger.log(booking.getIdBooking(), "ROOM", id, "UPDATE", "room_id", line.getRoom().getIdRoom(), roomId,
                request.reason(), user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "ROOM", id, "UPDATE", "check_in", line.getCheckIn(), checkIn,
                request.reason(), user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "ROOM", id, "UPDATE", "check_out", line.getCheckOut(), checkOut,
                request.reason(), user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "ROOM", id, "UPDATE", "unit_price", line.getUnitPrice(), unitPrice,
                request.reason(), user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "ROOM", id, "UPDATE", "reason", null, request.reason(),
                request.reason(), user.getIdUser(), operationId);
        line.setRoom(room);
        line.setCheckIn(checkIn);
        line.setCheckOut(checkOut);
        line.setUnitPrice(unitPrice);
        bookingRoomRepo.saveAndFlush(line);
        BookingBalance balance = bookingBalanceRepo.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        BigDecimal newTotal = balance.getTotal();
        if (request.expectedNewTotal() != null && request.expectedNewTotal().compareTo(newTotal) != 0)
            throw new UnprocessableEntityException("TOTAL_CHANGED", "Le total recalculé est différent du total attendu.");
        return bookingDetailService.get(booking.getIdBooking());
    }

    @Transactional
    public BookingDetailResponse extendRoom(Long id, BookingRoomExtendRequest request,
            Authentication authentication) {
        BookingRoom line = bookingRoomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne chambre a été modifiée par quelqu'un d'autre.");
        String status = line.getStatus() == null ? "" : line.getStatus().getCode();
        if (!Set.of("CONFIRMED", "CHECK_IN").contains(status))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED ou CHECK_IN peut être prolongée.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        LocalDate oldCheckOut = line.getCheckOut();
        LocalDate newCheckOut = request.newCheckOut();
        if (newCheckOut == null || oldCheckOut == null || line.getCheckIn() == null
                || !newCheckOut.isAfter(line.getCheckIn()))
            throw new ValidationException("Le départ doit laisser au moins une nuit.");
        if (newCheckOut.isBefore(LocalDate.now(clock)))
            throw new ValidationException("La date de départ ne peut pas être dans le passé.");
        if (request.reason() != null && request.reason().trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.");

        boolean extending = newCheckOut.isAfter(oldCheckOut);
        long changedNights = Math.abs(newCheckOut.toEpochDay() - oldCheckOut.toEpochDay());
        BigDecimal adjustment = line.getUnitPrice().multiply(BigDecimal.valueOf(changedNights));
        if (!extending) {
            adjustment = adjustment.negate();
            return saveExtension(line, booking, user, oldCheckOut, newCheckOut, adjustment, request);
        }

        String unavailableMode = normalizeUnavailableMode(request.ifUnavailable());
        if (!Set.of("REFUSE", "OTHER_ROOM").contains(unavailableMode))
            throw new ValidationException("ifUnavailable doit valoir REFUSE ou OTHER_ROOM.");
        List<BookingRoom> conflicts = bookingRoomRepo.findConflicts(line.getRoom().getIdRoom(), oldCheckOut,
                newCheckOut, id);
        if (!conflicts.isEmpty()) {
            if (!"OTHER_ROOM".equals(unavailableMode))
                throw unavailable(conflicts.get(0), oldCheckOut, newCheckOut);
            if (request.otherRoomId() == null)
                throw new ValidationException("Une autre chambre est obligatoire lorsque le mode OTHER_ROOM est choisi.");
            Room otherRoom = roomRepo.findById(request.otherRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id : " + request.otherRoomId()));
            assertRoomReservable(otherRoom);
            if (!Boolean.TRUE.equals(bookingRoomRepo.isDispo(otherRoom.getIdRoom(), oldCheckOut, newCheckOut, null)))
                throw unavailable(bookingRoomRepo.findConflicts(otherRoom.getIdRoom(), oldCheckOut, newCheckOut, null)
                        .stream().findFirst().orElse(null), oldCheckOut, newCheckOut);
            BigDecimal otherPrice = findRoomNightRate(otherRoom, oldCheckOut);
            BookingLineStatus confirmed = lineStatusRepo.findByCode("CONFIRMED");
            if (confirmed == null) throw new ConflictException("FORBIDDEN_TRANSITION", "Statut CONFIRMED introuvable.");
            BookingRoom addedLine = new BookingRoom();
            addedLine.setBooking(booking);
            addedLine.setRoom(otherRoom);
            addedLine.setCheckIn(oldCheckOut);
            addedLine.setCheckOut(newCheckOut);
            addedLine.setUnitPrice(otherPrice);
            addedLine.setStatus(confirmed);
            addedLine.setActive(true);
            addedLine = bookingRoomRepo.saveAndFlush(addedLine);
            saveHistory(booking, "ROOM", addedLine.getIdBookingRoom(), confirmed.getCode(), user);
            UUID operationId = UUID.randomUUID();
            changeLogger.log(booking.getIdBooking(), "ROOM", addedLine.getIdBookingRoom(), "UPDATE", "room_id", null,
                    otherRoom.getIdRoom(), request.reason(), user.getIdUser(), operationId);
            changeLogger.log(booking.getIdBooking(), "ROOM", addedLine.getIdBookingRoom(), "UPDATE", "check_in", null,
                    oldCheckOut, request.reason(), user.getIdUser(), operationId);
            changeLogger.log(booking.getIdBooking(), "ROOM", addedLine.getIdBookingRoom(), "UPDATE", "check_out", null,
                    newCheckOut, request.reason(), user.getIdUser(), operationId);
            changeLogger.log(booking.getIdBooking(), "ROOM", addedLine.getIdBookingRoom(), "UPDATE", "unit_price", null,
                    otherPrice, request.reason(), user.getIdUser(), operationId);
            adjustment = otherPrice.multiply(BigDecimal.valueOf(changedNights));
        } else {
                return saveExtension(line, booking, user, oldCheckOut, newCheckOut, adjustment, request);
        }
        return finishExtension(booking, adjustment, request.expectedNewTotal());
    }

        private BookingDetailResponse saveExtension(BookingRoom line, Booking booking, User user, LocalDate oldCheckOut,
            LocalDate newCheckOut, BigDecimal adjustment, BookingRoomExtendRequest request) {
        line.setCheckOut(newCheckOut);
        bookingRoomRepo.saveAndFlush(line);
        UUID operationId = UUID.randomUUID();
        changeLogger.log(booking.getIdBooking(), "ROOM", line.getIdBookingRoom(), "UPDATE", "check_out", oldCheckOut,
                newCheckOut, request.reason(), user.getIdUser(), operationId);
        changeLogger.log(booking.getIdBooking(), "ROOM", line.getIdBookingRoom(), "UPDATE", "reason", null,
                request.reason(), request.reason(), user.getIdUser(), operationId);
        return finishExtension(booking, adjustment, request.expectedNewTotal());
    }

    private BookingDetailResponse finishExtension(Booking booking, BigDecimal adjustment, BigDecimal expectedNewTotal) {
        BookingBalance balance = bookingBalanceRepo.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        if (expectedNewTotal != null && expectedNewTotal.compareTo(balance.getTotal()) != 0)
            throw new UnprocessableEntityException("TOTAL_CHANGED", "Le total recalculé est différent du total attendu.");
        String type = adjustment.signum() > 0 ? "SUPPLEMENT" : adjustment.signum() < 0 ? "ECONOMIE" : null;
        return bookingDetailService.get(booking.getIdBooking()).withAdjustment(adjustment, type);
    }

    @Transactional
    public BookingDetailResponse checkIn(Long id, BookingRoomCheckInRequest request,
            Authentication authentication) {
        BookingRoom line = bookingRoomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        requireOpenBooking(booking);
        requireVersion(line, request == null ? null : request.version(), authentication,
            request == null ? null : request.reason());
        String bookingStatus = booking.getStatus().getCode();
        if (!Set.of("PENDING", "CONFIRMED").contains(bookingStatus))
            throw new ConflictException("FORBIDDEN_TRANSITION", "L'arrivée est impossible dans l'état actuel du dossier.");
        if (!"CONFIRMED".equals(line.getStatus() == null ? null : line.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être enregistrée à l'arrivée.");

        OffsetDateTime checkInAt = request == null ? null : request.checkInAt();
        if (checkInAt == null) throw new ValidationException("La date et l'heure d'arrivée sont obligatoires.");
        if (line.getCheckIn() != null && checkInAt.toLocalDate().isBefore(line.getCheckIn()) && !isAdministrator(authentication))
            throw new ConflictException("FORBIDDEN", "Une arrivée anticipée nécessite le rôle Administrateur.");
        if (Boolean.TRUE.equals(bookingRoomRepo.existsOtherCheckIn(line.getRoom().getIdRoom(), id)))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Une autre ligne est déjà enregistrée à l'arrivée dans cette chambre.");

        User user = currentUser(authentication);
        String reason = request == null ? null : request.reason();
        bookingStatusService.changeLineStatus(line, "CHECK_IN", reason, user.getIdUser(), checkInAt);
        if (Set.of("PENDING", "CONFIRMED").contains(bookingStatus))
            bookingStatusService.changeBookingStatus(booking, "IN_PROGRESS", reason, user.getIdUser(), checkInAt);
        return bookingDetailService.get(booking.getIdBooking());
    }

    @Transactional
    public BookingDetailResponse checkOut(Long id, BookingRoomCheckOutRequest request,
            Authentication authentication) {
        BookingRoom line = bookingRoomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + id));
        Booking booking = line.getBooking();
        requireOpenBooking(booking);
        requireVersion(line, request == null ? null : request.version(), authentication,
            request == null ? null : request.reason());
        if (!"CHECK_IN".equals(line.getStatus() == null ? null : line.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CHECK_IN peut être enregistrée au départ.");

        String reason = request == null ? null : request.reason();
        OffsetDateTime checkOutAt = request == null ? null : request.checkOutAt();
        if (checkOutAt == null) throw new ValidationException("La date et l'heure de départ sont obligatoires.");
        LocalDate departureDate = checkOutAt.toLocalDate();
        if (line.getCheckIn() != null && departureDate.isBefore(line.getCheckIn()))
            throw new ValidationException("Le départ ne peut pas précéder la date d'arrivée.");
        boolean adjustDeparture = request != null && Boolean.TRUE.equals(request.adjustDeparture());
        if (adjustDeparture && line.getCheckOut() != null && line.getCheckOut().isAfter(departureDate)) {
            if (!isAdministrator(authentication))
                throw new ConflictException("FORBIDDEN", "L'ajustement du départ nécessite le rôle Administrateur.");
            if (line.getCheckIn() == null || !departureDate.isAfter(line.getCheckIn()))
                throw new ValidationException("Le départ doit laisser au moins une nuit.");
            UUID operationId = UUID.randomUUID();
            changeLogger.log(booking.getIdBooking(), "ROOM", id, "CHECK_OUT", "check_out", line.getCheckOut(), departureDate,
                    reason, currentUser(authentication).getIdUser(), operationId);
            line.setCheckOut(departureDate);
            bookingRoomRepo.saveAndFlush(line);
        }
        User user = currentUser(authentication);
        bookingStatusService.changeLineStatus(line, "CHECK_OUT", reason, user.getIdUser(), checkOutAt);
        return bookingDetailService.get(booking.getIdBooking());
    }

    private void requireOpenBooking(Booking booking) {
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
    }

    private void requireVersion(BookingRoom line, Integer version, Authentication authentication, String reason) {
        if (version == null || !version.equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne chambre a été modifiée par quelqu'un d'autre.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        if (reason != null && reason.trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.");
    }

    private User currentUser(Authentication authentication) {
        return userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_Administrateur".equals(authority.getAuthority()));
    }

    private String normalizeUnavailableMode(String mode) {
        return mode == null || mode.isBlank() ? "REFUSE" : mode.trim().toUpperCase();
    }

    private ConflictException unavailable(BookingRoom conflict, LocalDate from, LocalDate to) {
        LocalDate conflictFrom = conflict == null ? from : conflict.getCheckIn().isAfter(from) ? conflict.getCheckIn() : from;
        LocalDate conflictTo = conflict == null ? to : conflict.getCheckOut().isBefore(to) ? conflict.getCheckOut() : to;
        return new ConflictException("ROOM_UNAVAILABLE", "La chambre n'est pas disponible pour la partie ajoutée.",
                conflictFrom, conflictTo);
    }

    private void assertRoomReservable(Room room) {
        if (Boolean.TRUE.equals(room.getIsDeleted()))
            throw new UnprocessableEntityException("ROOM_UNAVAILABLE", "La chambre n'est pas réservable.");
        String status = room.getStatus() == null || room.getStatus().getName() == null
                ? "" : room.getStatus().getName().trim().toUpperCase();
        if (!"DISPONIBLE".equals(status))
            throw new UnprocessableEntityException("ROOM_UNAVAILABLE", "La chambre n'est pas réservable.");
    }

    @Transactional 
    public BookingRoomResponse createBooking(BookingRoomRequest req) {
        validateRequest(req);
        PaymentMethod paymentMethod = req.advance() == null ? null : paymentMethodRepo.findById(
            req.advance().methodId()).orElseThrow(() -> new ResourceNotFoundException(
                "Moyen de paiement introuvable avec l'id : " + req.advance().methodId()));
        PaymentKind advanceKind = req.advance() == null ? null : paymentKindRepo.findByCode("ADVANCE")
            .orElseThrow(() -> new ResourceNotFoundException("Type de paiement ADVANCE introuvable."));

        Customer customer = handleCustomer(req);
        User user = req.userId() == null ? null : userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'id : " + req.userId()));
        BookingStatus bookingStatus = findBookingStatus("PENDING");
        BookingLineStatus lineStatus = findLineStatus("CONFIRMED");

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(req.origin() == null ? BookingOrigin.LOCAL : req.origin());
        booking.setNotes(req.notes() == null ? null : req.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(bookingStatus);
        booking = bookingRepo.saveAndFlush(booking);

        List<BookingRoomResponse.RoomLine> roomLines = new ArrayList<>();
        List<BookingRoomResponse.ChargeLine> chargeLines = new ArrayList<>();
        for (RoomItem item : req.rooms()) {
            Room room = validateRoomItem(item);
            BigDecimal checkInNightPrice = findRoomNightRate(room, item.checkIn());
            long nights = item.checkOut().toEpochDay() - item.checkIn().toEpochDay();
            BigDecimal roomTotal = checkInNightPrice.multiply(BigDecimal.valueOf(nights));

            BookingRoom line = new BookingRoom();
            line.setBooking(booking);
            line.setRoom(room);
            line.setCheckIn(item.checkIn());
            line.setCheckOut(item.checkOut());
            line.setUnitPrice(checkInNightPrice);
            line.setStatus(lineStatus);
            line.setActive(true);
            line = bookingRoomRepo.saveAndFlush(line);
            roomLines.add(new BookingRoomResponse.RoomLine(line.getIdBookingRoom(), line.getRef(),
                    room.getIdRoom(), room.getRefRoom(), item.checkIn(), item.checkOut(),
                    checkInNightPrice, roomTotal));
            saveHistory(booking, "ROOM", line.getIdBookingRoom(), lineStatus.getCode(), user);
        }

        if (req.extras() != null) {
            for (BookingRoomRequest.ExtraItem extra : req.extras()) {
                BookingCharge charge = createCharge(booking, extra, user);
                BigDecimal chargeTotal = charge.getUnitPrice()
                        .multiply(BigDecimal.valueOf(charge.getQuantity()));
                chargeLines.add(new BookingRoomResponse.ChargeLine(charge.getIdBookingCharge(),
                        charge.getRef(), charge.getLabel(), charge.getQuantity(), charge.getUnitPrice(),
                        chargeTotal, charge.getDeposit()));
            }
        }

        if (req.advance() != null) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(paymentMethod);
            payment.setKind(advanceKind);
            payment.setAmount(req.advance().amount());
            payment.setReceivedByUser(user);
            paymentRepo.saveAndFlush(payment);
        }

        BookingBalance bookingBalance = bookingBalanceRepo.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException(
                        "La vue v_booking_balance n'a retourné aucun résultat pour la réservation."));
        BigDecimal total = bookingBalance.getTotal().add(bookingBalance.getDepositDue());
        if (req.expectedTotal() != null && req.expectedTotal().compareTo(total) != 0) {
            throw new UnprocessableEntityException("Le total calculé (" + total
                    + ") ne correspond pas au total attendu (" + req.expectedTotal() + ").");
        }

        if (req.advance() != null && req.advance().amount().compareTo(total) > 0) {
            throw new UnprocessableEntityException("L'acompte ne peut pas dépasser le total de la réservation.");
        }

        boolean confirmed = req.confirm() != null
            ? req.confirm()
            : req.advance() != null;
        bookingStatus = findBookingStatus(confirmed ? "CONFIRMED" : "PENDING");
        booking.setStatus(bookingStatus);
        bookingRepo.save(booking);
        BigDecimal balance = bookingBalance.getBalance().add(bookingBalance.getDepositDue());

        saveHistory(booking, null, null, bookingStatus.getCode(), user);
        return new BookingRoomResponse(booking.getIdBooking(), booking.getRef(), customer.getIdCustomer(),
            bookingStatus.getCode(), roomLines, chargeLines, total, balance,
            bookingBalance.getDepositDue(), bookingBalance.getDepositHeld());
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listBookings(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.max(1, Math.min(size, 100)),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = customerId == null
            ? bookingRepo.findAll(pageable)
            : bookingRepo.findAllByCustomer_IdCustomerOrderByCreatedAtDesc(customerId, pageable);

        List<Long> bookingIds = bookings.getContent().stream().map(Booking::getIdBooking).toList();
        Map<Long, BookingBalance> balances = bookingIds.isEmpty()
            ? Map.of()
            : bookingBalanceRepo.findAllById(bookingIds).stream()
                .collect(Collectors.toMap(BookingBalance::getIdBooking, balance -> balance));
        Map<Long, List<BookingLineResponse>> linesByBookingId = bookingIds.isEmpty()
            ? Map.of()
            : bookingLineViewRepo.findAllForBookings(bookingIds).stream()
                .collect(Collectors.groupingBy(
                    BookingLineView::getBookingId,
                    Collectors.mapping(BookingLineResponse::from, Collectors.toList())));

        return bookings.map(booking -> toBookingResponse(
            booking,
            requireBalance(booking.getIdBooking(), balances),
            linesByBookingId.getOrDefault(booking.getIdBooking(), List.of())));
    }

    @Transactional(readOnly = true)
        public BookingResponse getBooking(Long bookingId) {
        if (bookingId == null) {
            throw new ValidationException("L'identifiant de la réservation est obligatoire.");
        }
        Booking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Réservation introuvable avec l'id : " + bookingId));
        BookingBalance balance = bookingBalanceRepo.findById(bookingId)
            .orElseThrow(() -> new IllegalStateException(
                "La vue v_booking_balance n'a retourné aucun résultat pour la réservation " + bookingId));
        List<BookingLineResponse> lines = bookingLineViewRepo.findAllForBookings(List.of(bookingId)).stream()
            .map(BookingLineResponse::from)
            .toList();
        return toBookingResponse(booking, balance, lines);
    }

    BookingBalance requireBalance(Long bookingId, Map<Long, BookingBalance> balances) {
        BookingBalance balance = balances.get(bookingId);
        if (balance == null) {
            throw new IllegalStateException(
                "La vue v_booking_balance n'a retourné aucun résultat pour la réservation " + bookingId);
        }
        return balance;
    }

    BookingResponse toBookingResponse(Booking booking, BookingBalance balance,
        List<BookingLineResponse> lines) {
    List<BookingLineResponse> responseLines = markMainLine(lines);
    return new BookingResponse(
        booking.getIdBooking(),
        booking.getRef(),
        booking.getCustomer().getIdCustomer(),
        booking.getCustomer().getFirstName(),
        booking.getCustomer().getLastName(),
        booking.getCustomer().getPhone(),
        booking.getStatus().getCode(),
        booking.getStatus().getName(),
        booking.getStatus().getColorCode(),
        booking.getOrigin(),
        booking.getNotes(),
        booking.getCreatedAt(),
        booking.getUpdatedAt(),
        balance.getLinesTotal(),
        balance.getChargesTotal(),
        balance.getTotal().add(balance.getDepositDue()),
        balance.getPaid(),
        balance.getBalance().add(balance.getDepositDue()),
        balance.getDepositDue(),
        balance.getDepositHeld(),
        responseLines);
    }

    private List<BookingLineResponse> markMainLine(List<BookingLineResponse> lines) {
        if (lines.isEmpty()) return lines;
        int priority = lines.stream().mapToInt(line -> switch (line.lineType()) {
            case "SPACE" -> 0;
            case "MEETING_ROOM" -> 1;
            case "ROOM" -> 2;
            default -> 3;
        }).min().orElse(3);
        int selectedPriority = priority;
        BookingLineResponse main = lines.stream()
                .filter(line -> switch (line.lineType()) {
                    case "SPACE" -> selectedPriority == 0;
                    case "MEETING_ROOM" -> selectedPriority == 1;
                    case "ROOM" -> selectedPriority == 2;
                    default -> selectedPriority == 3;
                })
                .min(java.util.Comparator.comparing(BookingLineResponse::startsAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(BookingLineResponse::lineId))
                .orElseThrow();
        return lines.stream().map(line -> new BookingLineResponse(line.lineType(), line.lineId(),
                line.reference(), line.resourceId(), line.startsAt(), line.endsAt(), line.quantity(),
                line.unitPrice(), line.included(), line.amount(), line.statusId(), line.statusCode(),
                line.active(), line == main)).toList();
    }

    private void validateRequest(BookingRoomRequest req) {
        if (req == null) throw new ValidationException("Les données de réservation sont obligatoires.");
        if ((req.customerId() == null) == (req.newCustomer() == null)) {
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        }
        if (req.rooms() == null || req.rooms().isEmpty()) {
            throw new ValidationException("Au moins une chambre est requise.");
        }
        Set<Long> roomIds = new HashSet<>();
        for (RoomItem item : req.rooms()) {
            if (item == null || item.roomId() == null || item.checkIn() == null || item.checkOut() == null) {
                throw new ValidationException("Chaque chambre doit avoir un identifiant et une période.");
            }
            if (!roomIds.add(item.roomId())) {
                throw new ValidationException("Une chambre ne peut pas apparaître deux fois dans la réservation.");
            }
            if (!item.checkOut().isAfter(item.checkIn())) {
                throw new ValidationException("La date de départ doit être après la date d'arrivée.");
            }
            if (item.checkIn().isBefore(LocalDate.now())) {
                throw new ValidationException("La date d'arrivée ne peut pas être dans le passé.");
            }
        }
        if (req.extras() != null && req.extras().stream().anyMatch(
                extra -> extra == null || extra.quantity() == null || extra.quantity() < 1
                        || (extra.menuItemId() == null) == (extra.additionalServiceId() == null))) {
            throw new ValidationException("Chaque extra doit avoir une quantité positive et une seule source.");
        }
        if (req.advance() != null && (req.advance().methodId() == null || req.advance().amount() == null
                || req.advance().amount().signum() <= 0)) {
            throw new ValidationException("Le moyen et le montant positif de l'acompte sont obligatoires.");
        }
        if (req.expectedTotal() != null && req.expectedTotal().signum() < 0) {
            throw new ValidationException("Le total attendu ne peut pas être négatif.");
        }
    }
    
    private Room validateRoomItem(RoomItem item) {
        return validateRoomForBooking(item.roomId(), item.checkIn(), item.checkOut());
    }

    Room validateRoomForBooking(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Room chambre = roomRepo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre id:" + roomId + " introuvable !"));
        if (Boolean.TRUE.equals(chambre.getIsDeleted())) {
            throw new UnprocessableEntityException("La chambre " + chambre.getRefRoom() + " n'est pas réservable.");
        }
        String roomStatus = chambre.getStatus() == null || chambre.getStatus().getName() == null
                ? "" : chambre.getStatus().getName().trim().toUpperCase();
        if (!"DISPONIBLE".equals(roomStatus)) {
            throw new UnprocessableEntityException("La chambre " + chambre.getRefRoom() + " n'est pas réservable.");
        }
        if (!Boolean.TRUE.equals(bookingRoomRepo.isDispo(chambre.getIdRoom(), checkIn, checkOut, null))) {
            throw new UnprocessableEntityException("Chambre " + chambre.getRefRoom()
                    + " non disponible pour cette période.");
        }
        return chambre;
    }

    private Customer handleCustomer(BookingRoomRequest req) {
        return handleCustomer(req.customerId(), req.newCustomer());
    }

    Customer handleCustomer(Long customerId, BookingRoomRequest.NewCustomer details) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client introuvable avec l'id : " + customerId));
        }
        if (details == null || details.lastname() == null || details.lastname().isBlank()
                || details.firstname() == null || details.firstname().isBlank()) {
            throw new ValidationException("Le nom et le prénom du nouveau client sont obligatoires.");
        }
        String identityNumber = details.identificationNumber() == null
                ? null : details.identificationNumber().trim();
        if ((details.documentId() == null) != (identityNumber == null || identityNumber.isBlank())) {
            throw new ValidationException("Le type de document et le numéro d'identification doivent être fournis ensemble.");
        }
        IdentificationDocument document = null;
        if (details.documentId() != null) {
            document = docRepository.findById(details.documentId()).orElseThrow(
                    () -> new ResourceNotFoundException("Type de document introuvable avec l'id : "
                            + details.documentId()));
            Customer existing = customerRepository.findByDocument_IdDocumentAndIdentificationNumber(
                    details.documentId(), identityNumber).orElse(null);
            if (existing != null) return existing;
        }

        Customer customer = new Customer();
        customer.setLastName(details.lastname().trim());
        customer.setFirstName(details.firstname() == null ? null : details.firstname().trim());
        customer.setDateOfBirth(details.dateOfBirth());
        customer.setAddress(details.address() == null ? null : details.address().trim());
        customer.setEmail(details.email() == null ? null : details.email().trim());
        customer.setPhone(details.phone() == null ? null : details.phone().trim());
        customer.setDocument(document);
        customer.setIdentificationNumber(identityNumber);
        if (details.nationalityId() != null) {
            Nationality nationality = nationalityRepo.findById(details.nationalityId()).orElseThrow(
                    () -> new ResourceNotFoundException("Nationalité introuvable avec l'id : "
                            + details.nationalityId()));
            customer.setNationality(nationality);
        }
        return customerRepository.saveAndFlush(customer);
    }

    BigDecimal findRoomNightRate(Room room, LocalDate checkIn) {
        var checkInRate = roomTypeService.getApplicableNightRate(room.getType().getIdType(), checkIn);
        if (checkInRate == null) {
            throw new UnprocessableEntityException("NO_TARIFF",
                "Aucun tarif Nuit applicable à la date d'arrivée pour la chambre "
                    + room.getRefRoom() + " le " + checkIn + ".");
        }
        return checkInRate.getAmount();
    }

    private BookingCharge createCharge(Booking booking, BookingRoomRequest.ExtraItem extra, User user) {
        BookingCharge charge = new BookingCharge();
        charge.setBooking(booking);
        charge.setQuantity(extra.quantity());
        charge.setCreatedByUser(user);
        charge.setIncluded(false);

        if (extra.additionalServiceId() != null) {
            AdditionalService service = additionalServiceRepo.findById(extra.additionalServiceId().longValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
            if (!service.isActive()) {
                throw new UnprocessableEntityException("Le service additionnel " + service.getName()
                        + " n'est pas actif.");
            }
            String scope = service.getScope() == null || service.getScope().getCode() == null
                    ? "" : service.getScope().getCode().toUpperCase();
            if (!Set.of("ROOM", "ROOMS", "CHAMBRE", "CHAMBRES").contains(scope)) {
                throw new UnprocessableEntityException("Le service " + service.getName()
                        + " ne s'applique pas aux chambres.");
            }
            charge.setAdditionalService(service);
            charge.setLabel(service.getName());
            charge.setUnitPrice(service.getPrice());
            charge.setDeposit(service.isDeposit());
        } else {
            MenuItem menuItem = menuItemRepo.findById(extra.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Article du menu introuvable."));
            if (!Boolean.TRUE.equals(menuItem.getActive())) {
                throw new UnprocessableEntityException("L'article " + menuItem.getName() + " n'est pas actif.");
            }
            LocalDate today = LocalDate.now();
            MenuItemRate rate = menuItemRateRepo.findAllByMenuItem_IdMenuItemOrderByValidFromDesc(
                            menuItem.getIdMenuItem()).stream()
                    .filter(candidate -> !candidate.getValidFrom().isAfter(today)
                            && (candidate.getValidTo() == null || !candidate.getValidTo().isBefore(today)))
                    .max(java.util.Comparator.comparing(MenuItemRate::getValidFrom)
                            .thenComparing(MenuItemRate::getIdRate))
                    .orElseThrow(() -> new UnprocessableEntityException(
                            "Aucun prix applicable aujourd'hui pour " + menuItem.getName() + "."));
            charge.setMenuItem(menuItem);
            charge.setLabel(menuItem.getName());
            charge.setUnitPrice(rate.getAmount());
            charge.setDeposit(false);
        }
        return bookingChargeRepo.saveAndFlush(charge);
    }

    BookingStatus findBookingStatus(String code) {
        BookingStatus status = statusRepo.findByCode(code);
        if (status == null) throw new ResourceNotFoundException("Statut de réservation " + code + " introuvable.");
        return status;
    }

    BookingLineStatus findLineStatus(String code) {
        BookingLineStatus status = lineStatusRepo.findByCode(code);
        if (status == null) throw new ResourceNotFoundException("Statut de ligne " + code + " introuvable.");
        return status;
    }

    void saveHistory(Booking booking, String lineType, Long lineId, String newStatus, User user) {
        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(booking);
        history.setLineType(lineType);
        history.setLineId(lineId == null ? null : lineId.toString());
        history.setNewStatusCode(newStatus);
        history.setChangedByUser(user);
        historyRepo.save(history);
    }

    // private String newReference(String prefix) {
    //     return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    // }
}
