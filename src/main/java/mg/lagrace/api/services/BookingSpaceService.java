package mg.lagrace.api.services;

import mg.lagrace.api.dto.booking.room.BookingResponse;
import mg.lagrace.api.dto.booking.space.BookingSpaceRequest;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.*;
import mg.lagrace.api.models.enums.BookingOrigin;
import mg.lagrace.api.models.enums.CateringCondition;
import mg.lagrace.api.models.views.BookingBalance;
import mg.lagrace.api.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BookingSpaceService {
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
            UserRepository userRepository) {
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
    }

    @Transactional
    public BookingResponse createBooking(BookingSpaceRequest request, String username, boolean canOffer) {
        validateRequest(request, canOffer);
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
            throw new ConflictException("SPACE_ALREADY_BOOKED", "Cet espace est déjà réservé à cette date.");
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        Customer customer = bookingRoomService.handleCustomer(request.customerId(), request.newCustomer());

        BookingStatus initialStatus = bookingRoomService.findBookingStatus("PENDING");
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

        BookingBalance balance = bookingBalanceRepository.findById(booking.getIdBooking())
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat pour la réservation."));
        BigDecimal total = balance.getTotal().add(balance.getDepositDue());
        if (request.expectedTotal() != null && request.expectedTotal().compareTo(total) != 0) {
            throw new ConflictException("TOTAL_MISMATCH", "Le total calculé (" + total + ") ne correspond pas au total attendu (" + request.expectedTotal() + ").");
        }
        if (request.advance() != null && request.advance().amount().compareTo(total) > 0) {
            throw new ValidationException("L'acompte ne peut pas dépasser le total de la réservation.");
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

        boolean confirmed = request.advance() != null || Boolean.TRUE.equals(request.confirm());
        BookingStatus finalStatus = bookingRoomService.findBookingStatus(confirmed ? "CONFIRMED" : "PENDING");
        booking.setStatus(finalStatus);
        booking = bookingRepository.saveAndFlush(booking);
        bookingRoomService.saveHistory(booking, null, null, finalStatus.getCode(), user);
        for (HistoryLine line : historyLines) {
            bookingRoomService.saveHistory(booking, line.type(), line.id(), confirmedLineStatus.getCode(), user);
        }
        return bookingRoomService.getBooking(booking.getIdBooking());
    }

    private void validateRequest(BookingSpaceRequest request, boolean canOffer) {
        if (request == null || request.space() == null) throw new ValidationException("Les données de réservation espace sont obligatoires.");
        LocalDate todayInMadagascar = LocalDate.now(MADAGASCAR);
        if (request.space().eventDate() == null || request.space().eventDate().isBefore(todayInMadagascar)) {
            throw new ValidationException("La date de l'événement ne peut pas être dans le passé.");
        }
        if (request.space().spaceId() == null || request.space().guestsCount() == null || request.space().guestsCount() < 1
                || request.space().withCaterer() == null) {
            throw new ValidationException("L'espace, le nombre d'invités et le choix du traiteur sont obligatoires.");
        }
        if (request.customerId() == null == (request.newCustomer() == null)) {
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        }
        List<BookingSpaceRequest.BanquetItem> banquet = request.banquet() == null ? List.of() : request.banquet();
        if (request.space().withCaterer() && banquet.isEmpty()) throw new ValidationException("Avec traiteur, au moins un menu banquet est obligatoire.");
        if (!request.space().withCaterer() && !banquet.isEmpty()) throw new ValidationException("Un banquet ne peut pas être ajouté sans traiteur.");
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
            if (Boolean.TRUE.equals(item.included()) && !canOffer) throw new ValidationException("Seul un administrateur peut offrir une chambre.");
        }
        Set<Long> meetingRoomIds = new HashSet<>();
        if (request.meetingRooms() != null) for (BookingSpaceRequest.MeetingRoomItem item : request.meetingRooms()) {
            if (item == null || item.meetingRoomId() == null || item.serviceId() == null || item.serviceDate() == null) {
                throw new ValidationException("Chaque salle de réunion liée doit préciser la salle, le service et la date.");
            }
            if (item.serviceDate().isBefore(todayInMadagascar)) throw new ValidationException("La date d'une salle de réunion ne peut pas être dans le passé.");
            if (!meetingRoomIds.add(item.meetingRoomId())) throw new ValidationException("Une salle de réunion ne peut apparaître qu'une fois.");
            if (Boolean.TRUE.equals(item.included()) && !canOffer) throw new ValidationException("Seul un administrateur peut offrir une salle de réunion.");
        }
        Set<String> extraKeys = new HashSet<>();
        if (request.extras() != null) for (BookingSpaceRequest.ExtraItem item : request.extras()) {
            if (item == null || item.quantity() == null || item.quantity() < 1
                    || (item.additionalServiceId() == null) == (item.menuItemId() == null)) {
                throw new ValidationException("Chaque extra doit avoir une source unique et une quantité positive.");
            }
            String key = item.additionalServiceId() != null ? "S:" + item.additionalServiceId() : "M:" + item.menuItemId();
            if (!extraKeys.add(key)) throw new ValidationException("Un même extra ne peut apparaître qu'une fois.");
        }
        if (request.advance() != null && (request.advance().methodId() == null || request.advance().amount() == null
                || request.advance().amount().signum() <= 0)) throw new ValidationException("L'acompte doit avoir un moyen de paiement et un montant positif.");
        if (request.expectedTotal() != null && request.expectedTotal().signum() < 0) throw new ValidationException("Le total attendu ne peut pas être négatif.");
    }

    private SpaceRate findSpaceRate(Space space, LocalDate eventDate, int guestsCount) {
        List<SpaceRate> matches = spaceRateRepository.findApplicableBySpace(space.getIdSpace(), eventDate).stream()
                .filter(rate -> (rate.getMinPerson() == null || guestsCount >= rate.getMinPerson())
                        && (rate.getMaxPerson() == null || guestsCount <= rate.getMaxPerson()))
                .toList();
        if (matches.size() != 1) throw new UnprocessableEntityException("Aucun palier de tarif unique applicable pour cet espace, cette date et ce nombre d'invités.");
        return matches.get(0);
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
            if (matches.size() != 1) throw new UnprocessableEntityException("Aucun palier de banquet unique applicable pour le type " + item.menuTypeId() + ".");
            if (item.persons() > guestsCount) throw new ValidationException("Le nombre de personnes du banquet ne peut pas dépasser le nombre d'invités.");
            resolved.add(matches.get(0));
        }
        return resolved;
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
            if (!withCaterer && condition == CateringCondition.WITH_CATERER) {
                throw new UnprocessableEntityException("Le service " + service.getName() + " exige un traiteur.");
            }
            boolean included = withCaterer && condition == CateringCondition.WITHOUT_CATERER;
            if (included && service.isDeposit()) throw new UnprocessableEntityException("Une caution ne peut pas être incluse gratuitement.");
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
        }
        return charge;
    }

    private record HistoryLine(String type, Long id) {}
}