package mg.lagrace.api.booking.application;

import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRateRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRepository;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.payment.persistence.PaymentKindRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;
import mg.lagrace.api.payment.persistence.PaymentRepository;
import mg.lagrace.api.reference.Unit;

import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.booking.room.persistence.BookingRoomRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomRateRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomServiceRepository;
import mg.lagrace.api.catalog.space.persistence.BanquetMenuTypeRepository;
import mg.lagrace.api.catalog.space.persistence.BanquetRateRepository;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingOrigin;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.booking.room.BookingRoom;
import mg.lagrace.api.booking.space.BookingSpace;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.additionalservice.CateringCondition;
import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.meetingroom.MeetingRoomRate;
import mg.lagrace.api.catalog.meetingroom.MeetingRoomService;
import mg.lagrace.api.catalog.restaurant.MenuItem;
import mg.lagrace.api.catalog.restaurant.MenuItemRate;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.BanquetRate;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.catalog.space.SpaceEvent;
import mg.lagrace.api.catalog.space.SpaceEventRepository;
import mg.lagrace.api.catalog.space.SpaceRate;
import mg.lagrace.api.catalog.space.SpaceRateRepository;
import mg.lagrace.api.catalog.space.SpaceRepository;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.room.BookingRoomUpdateRequest;
import mg.lagrace.api.booking.dto.room.BookingResponse;
import mg.lagrace.api.booking.dto.BookingSpaceUpdateRequest;
import mg.lagrace.api.booking.dto.space.BookingSpaceRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.BusinessException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;

import mg.lagrace.api.payment.Payment;
import mg.lagrace.api.payment.PaymentKind;
import mg.lagrace.api.payment.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingSpaceService {
    @Autowired private BookingPermissionService permissions;
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private static final String SPACE_SCOPE = "SPACE";

    private final BookingRoomService bookingRoomService;
    private final BookingRepository bookingRepository;
    private final BookingSpaceRepository bookingSpaceRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingMeetingRoomRepository bookingMeetingRoomRepository;
    private final BookingChargeRepository bookingChargeRepository;
    private final BookingBalanceRepository bookingBalanceRepository;
    private final PaymentRepository paymentRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceEventRepository spaceEventRepository;
    private final SpaceRateRepository spaceRateRepository;
    private final BanquetRateRepository banquetRateRepository;
    private final BanquetMenuTypeRepository banquetMenuTypeRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemRateRepository menuItemRateRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingRoomServiceRepository meetingRoomServiceRepository;
    private final MeetingRoomRateRepository meetingRoomRateRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentKindRepository paymentKindRepository;
    private final UserRepository userRepository;
    private final BookingChangeLogger changeLogger;
    private final BookingDetailService bookingDetailService;

    public BookingSpaceService(BookingRoomService bookingRoomService,
            BookingRepository bookingRepository,
            BookingSpaceRepository bookingSpaceRepository,
            BookingRoomRepository bookingRoomRepository,
            BookingMeetingRoomRepository bookingMeetingRoomRepository,
            BookingChargeRepository bookingChargeRepository,
            BookingBalanceRepository bookingBalanceRepository,
            PaymentRepository paymentRepository,
            SpaceRepository spaceRepository,
            SpaceEventRepository spaceEventRepository,
            SpaceRateRepository spaceRateRepository,
            BanquetRateRepository banquetRateRepository,
            BanquetMenuTypeRepository banquetMenuTypeRepository,
            AdditionalServiceRepository additionalServiceRepository,
            MenuItemRepository menuItemRepository,
            MenuItemRateRepository menuItemRateRepository,
            MeetingRoomRepository meetingRoomRepository,
            MeetingRoomServiceRepository meetingRoomServiceRepository,
            MeetingRoomRateRepository meetingRoomRateRepository,
            PaymentMethodRepository paymentMethodRepository,
            PaymentKindRepository paymentKindRepository,
            UserRepository userRepository,
            BookingChangeLogger changeLogger,
            BookingDetailService bookingDetailService) {
        this.bookingRoomService = bookingRoomService;
        this.bookingRepository = bookingRepository;
        this.bookingSpaceRepository = bookingSpaceRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.bookingMeetingRoomRepository = bookingMeetingRoomRepository;
        this.bookingChargeRepository = bookingChargeRepository;
        this.bookingBalanceRepository = bookingBalanceRepository;
        this.paymentRepository = paymentRepository;
        this.spaceRepository = spaceRepository;
        this.spaceEventRepository = spaceEventRepository;
        this.spaceRateRepository = spaceRateRepository;
        this.banquetRateRepository = banquetRateRepository;
        this.banquetMenuTypeRepository = banquetMenuTypeRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemRateRepository = menuItemRateRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.meetingRoomServiceRepository = meetingRoomServiceRepository;
        this.meetingRoomRateRepository = meetingRoomRateRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentKindRepository = paymentKindRepository;
        this.userRepository = userRepository;
        this.changeLogger = changeLogger;
        this.bookingDetailService = bookingDetailService;
    }

    @Transactional
    public BookingDetailResponse createBooking(BookingSpaceRequest request, String username) {
        permissions.require(username, null, "BOOKING_UPDATE");
        if (request != null && request.advance() != null) permissions.require(username, null, "PAYMENT_COLLECT");
        if (request != null && request.deposit() != null) permissions.require(username, null, "DEPOSIT_COLLECT");
        if (request != null && Boolean.TRUE.equals(request.confirm())) permissions.require(username, null, "BOOKING_CONFIRM");
        validateRequest(request, username);
        LocalDate eventDate = request.space().eventDate();
        int guestsCount = request.space().guestsCount();
        boolean withCaterer = request.space().withCaterer();

        Space space = spaceRepository.findById(request.space().spaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Espace introuvable avec l'id : " + request.space().spaceId()));
        if (space.getCapacity() != null && guestsCount > space.getCapacity()) {
            throw new UnprocessableEntityException("La capacité de l'espace est de " + space.getCapacity() + " personnes.");
        }
        SpaceEvent eventType = request.space().eventTypeId() == null ? null
                : spaceEventRepository.findById(request.space().eventTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type d'événement introuvable avec l'id : " + request.space().eventTypeId()));
        if (bookingSpaceRepository.existsActiveBookingForSpaceAndDate(space.getIdSpace(), eventDate)) {
            throw new ConflictException("SPACE_UNAVAILABLE", "Cet espace est déjà réservé à cette date.", "space.eventDate", null);
        }

        SpaceRate spaceRate = findSpaceRate(space, eventDate, guestsCount);
        int spaceQuantity = quantityForSpaceRate(spaceRate, guestsCount);
        List<BanquetRate> banquetRates = resolveBanquetRates(request.banquet(), eventDate, guestsCount);
        PaymentMethod paymentMethod = request.advance() == null ? null
                : paymentMethodRepository.findById(request.advance().methodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable avec l'id : " + request.advance().methodId()));
        PaymentKind advanceKind = request.advance() == null ? null
                : paymentKindRepository.findByCode("ADVANCE")
                    .orElseThrow(() -> new ResourceNotFoundException("Type de paiement ADVANCE introuvable."));
        PaymentMethod depositMethod = request.deposit() == null ? null
            : paymentMethodRepository.findById(request.deposit().methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable avec l'id : " + request.deposit().methodId()));
        PaymentKind depositKind = request.deposit() == null ? null
            : paymentKindRepository.findByCode("DEPOSIT")
                .orElseThrow(() -> new ResourceNotFoundException("Type de paiement DEPOSIT introuvable."));
        AdditionalService depositService = request.deposit() == null ? null
            : resolveSpaceDepositService();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        Customer customer = bookingRoomService.handleCustomer(request.customerId(), request.newCustomer());

        boolean confirmed = request.advance() != null || request.deposit() != null || Boolean.TRUE.equals(request.confirm());
        BookingStatus initialStatus = bookingRoomService.findBookingStatus(confirmed ? "CONFIRMED" : "PENDING");
        BookingLineStatus confirmedLineStatus = bookingRoomService.findLineStatus("CONFIRMED");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(request.origin() == null ? BookingOrigin.LOCAL : request.origin());
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(initialStatus);
        booking = bookingRepository.saveAndFlush(booking);

        List<HistoryLine> historyLines = new ArrayList<>();
        BookingSpace bookingSpace = new BookingSpace();
        bookingSpace.setBooking(booking);
        bookingSpace.setSpace(space);
        bookingSpace.setEventType(eventType);
        bookingSpace.setEventDate(eventDate);
        bookingSpace.setGuestsCount(guestsCount);
        bookingSpace.setUnit(spaceRate.getUnit());
        bookingSpace.setQuantity(spaceQuantity);
        bookingSpace.setUnitPrice(spaceRate.getAmount());
        bookingSpace.setIncluded(false);
        bookingSpace.setWithCaterer(withCaterer);
        bookingSpace.setStatus(confirmedLineStatus);
        bookingSpace.setActive(true);
        bookingSpace = bookingSpaceRepository.saveAndFlush(bookingSpace);
        historyLines.add(new HistoryLine("SPACE", bookingSpace.getIdBookingSpace()));

        if (request.rooms() != null) {
            for (BookingSpaceRequest.RoomItem item : request.rooms()) {
                Room room = bookingRoomService.validateRoomForBooking(item.roomId(), item.checkIn(), item.checkOut());
                BigDecimal nightRate = bookingRoomService.findRoomNightRate(room, item.checkIn());
                BookingRoom line = new BookingRoom();
                line.setBooking(booking);
                line.setRoom(room);
                line.setCheckIn(item.checkIn());
                line.setCheckOut(item.checkOut());
                line.setUnitPrice(nightRate);
                line.setIncluded(Boolean.TRUE.equals(item.included()));
                line.setStatus(confirmedLineStatus);
                line.setActive(true);
                line = bookingRoomRepository.saveAndFlush(line);
                historyLines.add(new HistoryLine("ROOM", line.getIdBookingRoom()));
            }
        }

        if (request.meetingRooms() != null) {
            for (BookingSpaceRequest.MeetingRoomItem item : request.meetingRooms()) {
                MeetingRoom meetingRoom = meetingRoomRepository.findById(item.meetingRoomId())
                        .orElseThrow(() -> new ResourceNotFoundException("Salle de réunion introuvable avec l'id : " + item.meetingRoomId()));
                MeetingRoomService meetingService = meetingRoomServiceRepository.findById(item.serviceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Service de salle de réunion introuvable avec l'id : " + item.serviceId()));
                if (meetingRoom.getCapacity() != null && guestsCount > meetingRoom.getCapacity()) {
                    throw new UnprocessableEntityException("La capacité de la salle de réunion " + meetingRoom.getName() + " est insuffisante.");
                }
                if (bookingMeetingRoomRepository.existsActiveBookingForRoomAndDate(meetingRoom.getIdMeetingRoom(), item.serviceDate())) {
                    throw new ConflictException("MEETING_ROOM_ALREADY_BOOKED", "La salle de réunion " + meetingRoom.getName() + " est déjà réservée à cette date.");
                }
                List<MeetingRoomRate> applicableRates = meetingRoomRateRepository.findApplicableForBooking(
                        meetingRoom.getIdMeetingRoom(), meetingService.getIdService(), item.serviceDate());
                if (applicableRates.size() != 1) {
                    throw new UnprocessableEntityException("Aucun tarif unique applicable pour la salle de réunion " + meetingRoom.getName() + ".");
                }
                BookingMeetingRoom line = new BookingMeetingRoom();
                line.setBooking(booking);
                line.setMeetingRoom(meetingRoom);
                line.setMeetingRoomService(meetingService);
                line.setServiceDate(item.serviceDate());
                line.setGuestsCount(guestsCount);
                line.setQuantity(1);
                line.setUnitPrice(applicableRates.get(0).getAmount());
                line.setIncluded(Boolean.TRUE.equals(item.included()));
                line.setStatus(confirmedLineStatus);
                line.setActive(true);
                line = bookingMeetingRoomRepository.saveAndFlush(line);
                historyLines.add(new HistoryLine("MEETING_ROOM", line.getIdBookingMeetingRoom()));
            }
        }

        for (int index = 0; index < banquetRates.size(); index++) {
            BanquetRate rate = banquetRates.get(index);
            int persons = request.banquet().get(index).persons();
            BookingCharge charge = new BookingCharge();
            charge.setBooking(booking);
            charge.setBanquetRate(rate);
            charge.setLabel(rate.getMenuType().getName());
            charge.setQuantity(persons);
            charge.setUnitPrice(rate.getAmount());
            charge.setIncluded(false);
            charge.setDeposit(false);
            charge.setCreatedByUser(user);
            bookingChargeRepository.saveAndFlush(charge);
        }

        if (request.extras() != null) {
            for (BookingSpaceRequest.ExtraItem extra : request.extras()) {
                BookingCharge charge = createExtraCharge(booking, extra, user, eventDate, withCaterer);
                bookingChargeRepository.saveAndFlush(charge);
            }
        }

        if (depositService != null) {
            BookingCharge charge = new BookingCharge();
            charge.setBooking(booking);
            charge.setAdditionalService(depositService);
            charge.setLabel(depositService.getName());
            charge.setQuantity(1);
            charge.setUnitPrice(depositService.getPrice());
            charge.setIncluded(false);
            charge.setDeposit(true);
            charge.setCreatedByUser(user);
            bookingChargeRepository.saveAndFlush(charge);

            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(depositMethod);
            payment.setKind(depositKind);
            payment.setAmount(depositService.getPrice());
            payment.setReceivedByUser(user);
            paymentRepository.saveAndFlush(payment);
        }

        BookingBalance balance = bookingBalanceRepository.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat pour la réservation."));
        BigDecimal total = balance.getTotal();
        if (request.expectedTotal() != null && request.expectedTotal().compareTo(total) != 0) {
            throw new ConflictException("TOTAL_CHANGED", "Le total recalculé est différent du total attendu.", "expectedTotal", total);
        }
        BigDecimal remaining = total.subtract(balance.getPaid());
        if (request.advance() != null && request.advance().amount().compareTo(remaining) > 0) {
            throw new ValidationException("L'acompte ne peut pas dépasser le reste à payer hors caution.", "advance.amount");
        }

        if (request.advance() != null) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(paymentMethod);
            payment.setKind(advanceKind);
            payment.setAmount(request.advance().amount());
            payment.setReceivedByUser(user);
            paymentRepository.saveAndFlush(payment);
        }

        BookingStatus finalStatus = booking.getStatus();
        bookingRoomService.saveHistory(booking, null, null, finalStatus.getCode(), user);
        for (HistoryLine line : historyLines) {
            bookingRoomService.saveHistory(booking, line.type(), line.id(), confirmedLineStatus.getCode(), user);
        }
        return bookingDetailService.get(booking.getIdBooking());
    }

    public void validateAggregateSpaceUpdate(Long lineId, BookingSpaceUpdateRequest request) {
        BookingSpace line = bookingSpaceRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne espace introuvable avec l'id : " + lineId));
        Booking booking = line.getBooking();
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne espace a été modifiée par quelqu'un d'autre.");
        if (!"CONFIRMED".equals(line.getStatus() == null ? null : line.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être modifiée.");
        Long targetSpaceId = request.spaceId() == null ? line.getSpace().getIdSpace() : request.spaceId();
        Space targetSpace = spaceRepository.findById(targetSpaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espace introuvable avec l'id : " + targetSpaceId));
        LocalDate targetDate = request.eventDate() == null ? line.getEventDate() : request.eventDate();
        Integer targetGuests = request.guestsCount() == null ? line.getGuestsCount() : request.guestsCount();
        if (request.eventTypeId() != null && !spaceEventRepository.existsById(request.eventTypeId()))
            throw new ResourceNotFoundException("Type d'événement introuvable avec l'id : " + request.eventTypeId());
        if (targetDate == null || targetDate.isBefore(LocalDate.now(MADAGASCAR)))
            throw new ValidationException("La date de l'événement ne peut pas être dans le passé.", "eventDate");
        if (targetGuests == null || targetGuests < 1)
            throw new ValidationException("Le nombre d'invités doit être positif.", "guestsCount");
        if (request.reason() != null && request.reason().trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.", "reason");
        if (targetSpace.getCapacity() != null && targetGuests > targetSpace.getCapacity())
            throw new UnprocessableEntityException("La capacité de l'espace est de " + targetSpace.getCapacity() + " personnes.", "guestsCount");
        if ((!targetDate.equals(line.getEventDate()) || !targetSpaceId.equals(line.getSpace().getIdSpace()))
                && bookingSpaceRepository.existsActiveBookingForSpaceAndDate(targetSpaceId, targetDate, lineId))
            throw new ConflictException("SPACE_UNAVAILABLE", "Cet espace est déjà réservé à cette date.", "eventDate", null);
        SpaceRate rate = findSpaceRate(targetSpace, targetDate, targetGuests);
        quantityForSpaceRate(rate, targetGuests);
    }

    @Transactional
    public BookingResponse updateSpace(Long lineId, BookingSpaceUpdateRequest request,
            org.springframework.security.core.Authentication authentication) {
        return updateSpaceInternal(lineId, request, authentication, true, Set.of());
        }

        @Transactional
        public BookingResponse updateSpaceForAggregate(Long lineId,
            BookingSpaceUpdateRequest request,
            org.springframework.security.core.Authentication authentication, Set<Long> deferredBanquetChargeIds) {
        return updateSpaceInternal(lineId, request, authentication, false,
            deferredBanquetChargeIds == null ? Set.of() : deferredBanquetChargeIds);
        }

        private BookingResponse updateSpaceInternal(Long lineId,
            BookingSpaceUpdateRequest request,
            org.springframework.security.core.Authentication authentication, boolean validateCurrentCatering,
            Set<Long> deferredBanquetChargeIds) {
        BookingSpace line = bookingSpaceRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne espace introuvable avec l'id : " + lineId));
        Booking booking = line.getBooking();
        if (booking == null || booking.getStatus() == null
                || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (request == null || request.version() == null || !request.version().equals(line.getVersion()))
            throw new ConflictException("STALE_VERSION", "La ligne espace a été modifiée par quelqu'un d'autre.");
        if (!"CONFIRMED".equals(line.getStatus() == null ? null : line.getStatus().getCode()))
            throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être modifiée.");
        if (authentication == null)
            throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));

        Long targetSpaceId = request.spaceId() != null ? request.spaceId() : line.getSpace().getIdSpace();
        Space targetSpace = spaceRepository.findById(targetSpaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espace introuvable avec l'id : " + targetSpaceId));
        LocalDate targetDate = request.eventDate() != null ? request.eventDate() : line.getEventDate();
        Integer targetGuests = request.guestsCount() != null ? request.guestsCount() : line.getGuestsCount();
        Boolean targetWithCaterer = request.withCaterer() != null ? request.withCaterer() : line.getWithCaterer();
        SpaceEvent targetEventType = request.eventTypeId() == null ? line.getEventType()
                : spaceEventRepository.findById(request.eventTypeId()).orElseThrow(
                        () -> new ResourceNotFoundException("Type d'événement introuvable avec l'id : " + request.eventTypeId()));

        if (targetDate == null || targetDate.isBefore(LocalDate.now(MADAGASCAR)))
            throw new ValidationException("La date de l'événement ne peut pas être dans le passé.");
        if (request.reason() != null && request.reason().trim().length() > 255)
            throw new ValidationException("Le motif ne peut pas dépasser 255 caractères.");
        if (targetGuests == null || targetGuests < 1 || targetWithCaterer == null)
            throw new ValidationException("Le nombre d'invités et le choix du traiteur sont obligatoires.");
        if (targetSpace.getCapacity() != null && targetGuests > targetSpace.getCapacity())
            throw new UnprocessableEntityException("La capacité de l'espace est de " + targetSpace.getCapacity() + " personnes.");
        boolean dateChanged = !targetDate.equals(line.getEventDate());
        boolean spaceChanged = !targetSpaceId.equals(line.getSpace().getIdSpace());
        if ((dateChanged || spaceChanged)
                && bookingSpaceRepository.existsActiveBookingForSpaceAndDate(targetSpaceId, targetDate, lineId))
            throw new ConflictException("SPACE_UNAVAILABLE", "Cet espace est déjà réservé à cette date.");

        boolean withCaterer = Boolean.TRUE.equals(targetWithCaterer);
        List<BookingCharge> activeCharges = bookingChargeRepository.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .filter(charge -> charge.getVoidedAt() == null).toList();
        List<BookingCharge> banquetCharges = activeCharges.stream().filter(charge -> charge.getBanquetRate() != null).toList();
        if (validateCurrentCatering) {
            if ((withCaterer && banquetCharges.isEmpty()) || (!withCaterer && !banquetCharges.isEmpty()))
                throw catererRule("Le mode traiteur doit correspondre aux menus banquet actifs.");
            Set<Long> menuTypeIds = new HashSet<>();
            for (BookingCharge charge : banquetCharges) {
                BanquetRate rate = charge.getBanquetRate();
                if (charge.getQuantity() > targetGuests || !menuTypeIds.add(rate.getMenuType().getIdMenuType()))
                    throw catererRule("Les quantités et types de menu doivent rester compatibles avec le nombre d'invités.");
            }
        }
        List<BanquetRate> newBanquetRates = new ArrayList<>();
        for (BookingCharge charge : banquetCharges) {
            if (deferredBanquetChargeIds.contains(charge.getIdBookingCharge())) {
                newBanquetRates.add(null);
                continue;
            }
            List<BanquetRate> matches = banquetRateRepository.findApplicableByMenuType(
                charge.getBanquetRate().getMenuType().getIdMenuType(), targetDate).stream()
                    .filter(rate -> charge.getQuantity() >= rate.getMinPersons()
                            && (rate.getMaxPersons() == null || charge.getQuantity() <= rate.getMaxPersons()))
                    .toList();
            if (matches.size() != 1)
                throw new UnprocessableEntityException("NO_TARIFF", "Aucun palier de banquet applicable à la nouvelle date.");
            newBanquetRates.add(matches.get(0));
        }
        List<BookingCharge> serviceCharges = activeCharges.stream().filter(charge -> charge.getAdditionalService() != null
                && charge.getAdditionalService().getScope() != null
                && SPACE_SCOPE.equalsIgnoreCase(charge.getAdditionalService().getScope().getCode())).toList();
        for (BookingCharge charge : serviceCharges) {
            AdditionalService service = charge.getAdditionalService();
            CateringCondition condition = service.getCateringCondition();
            if (validateCurrentCatering && ((withCaterer && condition == CateringCondition.WITHOUT_CATERER)
                    || (!withCaterer && condition == CateringCondition.WITH_CATERER))
                )
                throw catererRule("Le service " + service.getName() + " n'est pas compatible avec le mode traiteur choisi.");
        }

        SpaceRate rate = findSpaceRate(targetSpace, targetDate, targetGuests);
        int quantity = quantityForSpaceRate(rate, targetGuests);
        BookingBalance previousBalance = bookingBalanceRepository.findById(booking.getIdBooking())
            .orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        BigDecimal previousTotal = previousBalance.getTotal();
        UUID operationId = UUID.randomUUID();
        String reason = request.reason() == null ? null : request.reason().trim();
        logSpaceChange(booking.getIdBooking(), lineId, "space_id", line.getSpace().getIdSpace(), targetSpaceId, reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "event_date", line.getEventDate(), targetDate, reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "event_type_id",
            line.getEventType() == null ? null : line.getEventType().getIdEvent(),
            targetEventType == null ? null : targetEventType.getIdEvent(), reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "guests_count", line.getGuestsCount(), targetGuests, reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "with_caterer", line.getWithCaterer(), withCaterer, reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "quantity", line.getQuantity(), quantity, reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "unit_price", line.getUnitPrice(), rate.getAmount(), reason, user, operationId);
        logSpaceChange(booking.getIdBooking(), lineId, "reason", null, reason, reason, user, operationId);
        line.setSpace(targetSpace);
        line.setEventDate(targetDate);
        line.setEventType(targetEventType);
        line.setGuestsCount(targetGuests);
        line.setWithCaterer(withCaterer);
        line.setUnit(rate.getUnit());
        line.setQuantity(quantity);
        line.setUnitPrice(rate.getAmount());
        bookingSpaceRepository.saveAndFlush(line);
        for (int index = 0; index < banquetCharges.size(); index++) {
            BookingCharge charge = banquetCharges.get(index);
            BanquetRate newRate = newBanquetRates.get(index);
            if (newRate == null) continue;
            charge.setBanquetRate(newRate);
            charge.setUnitPrice(newRate.getAmount());
            charge.setLabel(newRate.getMenuType().getName());
            bookingChargeRepository.save(charge);
        }
        for (BookingCharge charge : serviceCharges) {
            AdditionalService service = charge.getAdditionalService();
            if (service.isFreeWithCaterer()) charge.setIncluded(withCaterer);
        }
        if (request.shiftLinked() != null) {
            Set<Long> shiftedRoomIds = new HashSet<>();
            for (BookingSpaceUpdateRequest.ShiftLinkedRoom shift : request.shiftLinked()) {
                if (shift == null || shift.bookingRoomId() == null || shift.newCheckIn() == null
                        || shift.newCheckOut() == null || !shift.newCheckOut().isAfter(shift.newCheckIn()))
                    throw new ValidationException("Chaque chambre liée doit avoir un identifiant et une période valide.", "shiftLinked");
                if (!shiftedRoomIds.add(shift.bookingRoomId()))
                    throw new ValidationException("Une chambre liée ne peut apparaître qu'une fois.", "shiftLinked");
                BookingRoom linkedRoom = bookingRoomRepository.findById(shift.bookingRoomId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ligne chambre introuvable avec l'id : " + shift.bookingRoomId()));
                if (linkedRoom.getBooking() == null || !booking.getIdBooking().equals(linkedRoom.getBooking().getIdBooking()))
                    throw new ValidationException("La chambre liée n'appartient pas à cette réservation.", "shiftLinked");
                bookingRoomService.updateRoom(linkedRoom.getIdBookingRoom(), new BookingRoomUpdateRequest(
                        linkedRoom.getVersion(), null, shift.newCheckIn(), shift.newCheckOut(), reason, null), authentication);
            }
        }
        BookingBalance balance = bookingBalanceRepository.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        BigDecimal newTotal = balance.getTotal();
        if (request.expectedNewTotal() != null && request.expectedNewTotal().compareTo(newTotal) != 0)
            throw new ConflictException("IMPACT_CONFIRMATION_REQUIRED", "Le total recalculé est différent du total attendu.",
                    "expectedNewTotal", newTotal);
        if (previousTotal.compareTo(newTotal) != 0 && !Boolean.TRUE.equals(request.confirmImpact()))
            throw new ConflictException("IMPACT_CONFIRMATION_REQUIRED", "Le total de la réservation va être modifié. Confirmez le nouveau total.",
                    "expectedNewTotal", newTotal);
        return bookingRoomService.getBooking(booking.getIdBooking());
    }

    private void logSpaceChange(Long bookingId, Long lineId, String field, Object oldValue, Object newValue,
            String reason, User user, UUID operationId) {
        if (java.util.Objects.equals(oldValue, newValue)) return;
        changeLogger.log(bookingId, "SPACE", lineId, "UPDATE", field, oldValue, newValue,
                reason, user.getIdUser(), operationId);
    }

    private void validateRequest(BookingSpaceRequest request, String username) {
        if (request == null || request.space() == null) throw new ValidationException("Les données de réservation espace sont obligatoires.", "space");
        LocalDate todayInMadagascar = LocalDate.now(MADAGASCAR);
        if (request.space().eventDate() == null || request.space().eventDate().isBefore(todayInMadagascar)) {
            throw new ValidationException("La date de l'événement ne peut pas être dans le passé.", "space.eventDate");
        }
        if (request.space().spaceId() == null || request.space().guestsCount() == null || request.space().guestsCount() < 1
                || request.space().withCaterer() == null) {
            throw new ValidationException("L'espace, le nombre d'invités et le choix du traiteur sont obligatoires.", "space");
        }
        if (request.customerId() == null == (request.newCustomer() == null)) {
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.", "customerId");
        }
        List<BookingSpaceRequest.BanquetItem> banquet = request.banquet() == null ? List.of() : request.banquet();
        if (request.space().withCaterer() && banquet.isEmpty()) throw catererRule("Avec traiteur, au moins un menu banquet est obligatoire.");
        if (!request.space().withCaterer() && !banquet.isEmpty()) throw catererRule("Un banquet ne peut pas être ajouté sans traiteur.");
        Set<Long> menuTypeIds = new HashSet<>();
        for (BookingSpaceRequest.BanquetItem item : banquet) {
            if (item == null || item.menuTypeId() == null || item.persons() == null || item.persons() < 1
                    || item.persons() > request.space().guestsCount()) throw new ValidationException("Chaque menu banquet doit avoir un nombre de personnes entre 1 et le nombre d'invités.");
            if (!menuTypeIds.add(item.menuTypeId())) throw new ValidationException("Un type de menu banquet ne peut apparaître qu'une fois.");
        }
        Set<Long> roomIds = new HashSet<>();
        if (request.rooms() != null) for (BookingSpaceRequest.RoomItem item : request.rooms()) {
            if (item == null || item.roomId() == null || item.checkIn() == null || item.checkOut() == null || !item.checkOut().isAfter(item.checkIn())) {
                throw new ValidationException("Chaque chambre liée doit avoir un identifiant et une période valide.");
            }
            if (item.checkIn().isBefore(todayInMadagascar)) throw new ValidationException("L'arrivée d'une chambre liée ne peut pas être dans le passé.");
            if (!roomIds.add(item.roomId())) throw new ValidationException("Une chambre ne peut apparaître qu'une fois.");
            if (Boolean.TRUE.equals(item.included())) permissions.require(username, "rooms.included", "ROOM_SET_INCLUDED");
        }
        Set<Long> meetingRoomIds = new HashSet<>();
        if (request.meetingRooms() != null) for (BookingSpaceRequest.MeetingRoomItem item : request.meetingRooms()) {
            if (item == null || item.meetingRoomId() == null || item.serviceId() == null || item.serviceDate() == null) {
                throw new ValidationException("Chaque salle de réunion liée doit préciser la salle, le service et la date.");
            }
            if (item.serviceDate().isBefore(todayInMadagascar)) throw new ValidationException("La date d'une salle de réunion ne peut pas être dans le passé.");
            if (!meetingRoomIds.add(item.meetingRoomId())) throw new ValidationException("Une salle de réunion ne peut apparaître qu'une fois.");
            if (Boolean.TRUE.equals(item.included())) permissions.require(username, "meetingRooms.included", "ROOM_SET_INCLUDED");
        }
        Set<String> extraKeys = new HashSet<>();
        if (request.extras() != null) for (BookingSpaceRequest.ExtraItem item : request.extras()) {
            if (item == null || item.quantity() == null || item.quantity() < 1
                    || (item.additionalServiceId() == null) == (item.menuItemId() == null)) {
                throw new ValidationException("Chaque extra doit avoir une source unique et une quantité positive.");
            }
            String key = item.additionalServiceId() != null ? "S:" + item.additionalServiceId() : "M:" + item.menuItemId();
            if (!extraKeys.add(key)) throw new ValidationException("Un même extra ne peut apparaître qu'une fois.");
            if (Boolean.TRUE.equals(item.included())) permissions.require(username, "extras.included", "CHARGE_SET_INCLUDED");
        }
        if (request.advance() != null && (request.advance().methodId() == null || request.advance().amount() == null
            || request.advance().amount().signum() <= 0)) throw new ValidationException("L'acompte doit avoir un moyen de paiement et un montant positif.", "advance");
        if (request.deposit() != null && request.deposit().methodId() == null)
            throw new ValidationException("Le moyen de paiement de la caution est obligatoire.", "deposit.methodId");
        if (request.expectedTotal() != null && request.expectedTotal().signum() < 0) throw new ValidationException("Le total attendu ne peut pas être négatif.", "expectedTotal");
    }

    private SpaceRate findSpaceRate(Space space, LocalDate eventDate, int guestsCount) {
        List<SpaceRate> matches = spaceRateRepository.findApplicableBySpace(space.getIdSpace(), eventDate).stream()
                .filter(rate -> (rate.getMinPerson() == null || guestsCount >= rate.getMinPerson())
                        && (rate.getMaxPerson() == null || guestsCount <= rate.getMaxPerson()))
                .toList();
        return matches.stream()
            .max(java.util.Comparator
                .comparing((SpaceRate rate) -> rate.getValidTo() != null)
                .thenComparing(SpaceRate::getValidFrom)
                .thenComparing(SpaceRate::getIdRate))
            .orElseThrow(() -> new UnprocessableEntityException("NO_TARIFF",
                "Aucun palier de tarif applicable pour cet espace, cette date et ce nombre d'invités.", "space.guestsCount"));
    }

    private int quantityForSpaceRate(SpaceRate rate, int guestsCount) {
        String unit = rate.getUnit().getName().trim().toLowerCase(Locale.ROOT);
        if (unit.equals("forfait")) return 1;
        if (unit.equals("par personne")) return guestsCount;
        throw new UnprocessableEntityException("L'unité " + rate.getUnit().getName() + " n'est pas prise en charge pour la location d'espace.");
    }

    private List<BanquetRate> resolveBanquetRates(List<BookingSpaceRequest.BanquetItem> items, LocalDate eventDate, int guestsCount) {
        if (items == null) return List.of();
        List<BanquetRate> resolved = new ArrayList<>();
        for (BookingSpaceRequest.BanquetItem item : items) {
            banquetMenuTypeRepository.findById(item.menuTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Type de menu banquet introuvable avec l'id : " + item.menuTypeId()));
            List<BanquetRate> matches = banquetRateRepository.findApplicableByMenuType(item.menuTypeId(), eventDate).stream()
                    .filter(rate -> item.persons() >= rate.getMinPersons()
                            && (rate.getMaxPersons() == null || item.persons() <= rate.getMaxPersons()))
                    .toList();
            if (matches.size() != 1) throw new UnprocessableEntityException("NO_TARIFF", "Aucun palier de banquet unique applicable pour le type " + item.menuTypeId() + ".", "banquet");
            if (item.persons() > guestsCount) throw new ValidationException("Le nombre de personnes du banquet ne peut pas dépasser le nombre d'invités.");
            resolved.add(matches.get(0));
        }
        return resolved;
    }

    private AdditionalService resolveSpaceDepositService() {
        List<AdditionalService> deposits = additionalServiceRepository.findActiveSpaceDeposits();
        if (deposits.size() != 1)
            throw new ConflictException("DEPOSIT_CONFIGURATION", "Un seul service de caution actif doit être configuré pour les espaces.");
        return deposits.get(0);
    }

    private BookingCharge createExtraCharge(Booking booking, BookingSpaceRequest.ExtraItem extra,
            User user, LocalDate eventDate, boolean withCaterer) {
        BookingCharge charge = new BookingCharge();
        charge.setBooking(booking);
        charge.setQuantity(extra.quantity());
        charge.setCreatedByUser(user);
        charge.setIncluded(false);
        if (extra.additionalServiceId() != null) {
            AdditionalService service = additionalServiceRepository.findById(extra.additionalServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable avec l'id : " + extra.additionalServiceId()));
            if (!service.isActive()) throw new UnprocessableEntityException("Le service " + service.getName() + " n'est pas actif.");
            if (service.getScope() == null || !SPACE_SCOPE.equalsIgnoreCase(service.getScope().getCode())) {
                throw new UnprocessableEntityException("Le service " + service.getName() + " ne s'applique pas à un espace.");
            }
            CateringCondition condition = service.getCateringCondition();
            if ((withCaterer && condition == CateringCondition.WITHOUT_CATERER)
                    || (!withCaterer && condition == CateringCondition.WITH_CATERER)) {
                throw catererRule("Le service " + service.getName() + " n'est pas compatible avec le mode traiteur choisi.");
            }
            if (service.isDeposit()) throw new UnprocessableEntityException("La caution est ajoutée uniquement lors de son encaissement.");
            boolean included = Boolean.TRUE.equals(extra.included()) || (withCaterer && service.isFreeWithCaterer());
            charge.setAdditionalService(service);
            charge.setLabel(service.getName());
            charge.setUnitPrice(service.getPrice());
            charge.setDeposit(service.isDeposit());
            charge.setIncluded(included);
        } else {
            MenuItem item = menuItemRepository.findById(extra.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Article de menu introuvable avec l'id : " + extra.menuItemId()));
            if (!Boolean.TRUE.equals(item.getActive())) throw new UnprocessableEntityException("L'article " + item.getName() + " n'est pas actif.");
            MenuItemRate rate = menuItemRateRepository.findAllByMenuItem_IdMenuItemOrderByValidFromDesc(item.getIdMenuItem()).stream()
                    .filter(candidate -> !candidate.getValidFrom().isAfter(eventDate)
                            && (candidate.getValidTo() == null || !candidate.getValidTo().isBefore(eventDate)))
                    .max(java.util.Comparator.comparing(MenuItemRate::getValidFrom).thenComparing(MenuItemRate::getIdRate))
                    .orElseThrow(() -> new UnprocessableEntityException("Aucun tarif applicable à la date de l'événement pour " + item.getName() + "."));
            charge.setMenuItem(item);
            charge.setLabel(item.getName());
            charge.setUnitPrice(rate.getAmount());
            charge.setDeposit(false);
            charge.setIncluded(Boolean.TRUE.equals(extra.included()));
        }
        return charge;
    }

    private UnprocessableEntityException catererRule(String message) {
        return new UnprocessableEntityException("CATERER_RULE", message, "space.withCaterer");
    }

    private record HistoryLine(String type, Long id) {}
}
