package mg.lagrace.api.booking.application;

import mg.lagrace.api.booking.core.BookingOrigin;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.reference.Unit;

import mg.lagrace.api.booking.dto.room.BookingRoomRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import mg.lagrace.api.booking.dto.meeting.BookingMeetingRoomRequest;
import mg.lagrace.api.booking.dto.meeting.BookingMeetingRoomUpdateRequest;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.application.BookingPermissionService;
import mg.lagrace.api.booking.charge.BookingCharge;
import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.core.BookingLineStatus;
import mg.lagrace.api.booking.core.BookingRepository;
import mg.lagrace.api.booking.core.BookingStatus;
import mg.lagrace.api.booking.meetingroom.BookingMeetingRoom;
import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.meetingroom.MeetingRoom;
import mg.lagrace.api.catalog.meetingroom.MeetingRoomRate;
import mg.lagrace.api.catalog.meetingroom.MeetingRoomService;
import mg.lagrace.api.catalog.restaurant.MenuItem;
import mg.lagrace.api.catalog.restaurant.MenuItemRate;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.dashboard.BookingBalance;
import mg.lagrace.api.booking.dto.BookingDetailResponse;
import mg.lagrace.api.booking.dto.room.BookingResponse;
import mg.lagrace.api.booking.dto.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.dashboard.persistence.BookingBalanceRepository;
import mg.lagrace.api.booking.charge.persistence.BookingChargeRepository;
import mg.lagrace.api.booking.meetingroom.persistence.BookingMeetingRoomRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomRateRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomRepository;
import mg.lagrace.api.catalog.meetingroom.persistence.MeetingRoomServiceRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRateRepository;
import mg.lagrace.api.catalog.restaurant.persistence.MenuItemRepository;
import mg.lagrace.api.payment.persistence.PaymentKindRepository;
import mg.lagrace.api.payment.persistence.PaymentMethodRepository;
import mg.lagrace.api.payment.persistence.PaymentRepository;
import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.payment.Payment;
import mg.lagrace.api.payment.PaymentMethod;

@Service
public class BookingMeetingRoomService {
    @Autowired private BookingPermissionService permissions;
    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");
    private static final String MEETING_SCOPE = "MEETING_ROOM";

    private final BookingRoomService common;
    private final BookingRepository bookingRepository;
    private final BookingMeetingRoomRepository bookingMeetingRoomRepository;
    private final BookingChargeRepository bookingChargeRepository;
    private final BookingBalanceRepository bookingBalanceRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingRoomServiceRepository meetingRoomServiceRepository;
    private final MeetingRoomRateRepository meetingRoomRateRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemRateRepository menuItemRateRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentKindRepository paymentKindRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BookingExtraService extraService;
    private final BookingDetailService details;
    private final BookingChangeLogger changeLogger;
    private final Clock clock;

    public BookingMeetingRoomService(BookingRoomService common, BookingRepository bookingRepository,
            BookingMeetingRoomRepository bookingMeetingRoomRepository, BookingChargeRepository bookingChargeRepository,
            BookingBalanceRepository bookingBalanceRepository, MeetingRoomRepository meetingRoomRepository,
            MeetingRoomServiceRepository meetingRoomServiceRepository, MeetingRoomRateRepository meetingRoomRateRepository,
            AdditionalServiceRepository additionalServiceRepository, MenuItemRepository menuItemRepository,
            MenuItemRateRepository menuItemRateRepository, PaymentMethodRepository paymentMethodRepository,
            PaymentKindRepository paymentKindRepository, PaymentRepository paymentRepository,
            UserRepository userRepository, BookingExtraService extraService,
            BookingDetailService details, BookingChangeLogger changeLogger, Clock clock) {
        this.common = common;
        this.bookingRepository = bookingRepository;
        this.bookingMeetingRoomRepository = bookingMeetingRoomRepository;
        this.bookingChargeRepository = bookingChargeRepository;
        this.bookingBalanceRepository = bookingBalanceRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.meetingRoomServiceRepository = meetingRoomServiceRepository;
        this.meetingRoomRateRepository = meetingRoomRateRepository;
        this.additionalServiceRepository = additionalServiceRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemRateRepository = menuItemRateRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentKindRepository = paymentKindRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.extraService = extraService;
        this.details = details;
        this.changeLogger = changeLogger;
        this.clock = clock;
    }

    @Transactional
    public BookingDetailResponse update(Long id, BookingMeetingRoomUpdateRequest request, Authentication authentication) {
        BookingMeetingRoom line = bookingMeetingRoomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ligne salle de réunion introuvable."));
        Booking booking = line.getBooking();
        if (request == null || request.version() == null || !request.version().equals(line.getVersion())) throw new ConflictException("STALE_VERSION", "La ligne salle de réunion a été modifiée par quelqu'un d'autre.");
        if (booking == null || booking.getStatus() == null || Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode())) throw new ConflictException("FORBIDDEN", "Le dossier est terminé ou annulé.");
        if (line.getStatus() == null || !"CONFIRMED".equals(line.getStatus().getCode())) throw new ConflictException("FORBIDDEN_TRANSITION", "Seule une ligne CONFIRMED peut être modifiée.");
        if (authentication == null) throw new ConflictException("FORBIDDEN", "Utilisateur non authentifié.");
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        MeetingRoom room = request.meetingRoomId() == null ? line.getMeetingRoom() : meetingRoomRepository.findById(request.meetingRoomId()).orElseThrow(() -> new ResourceNotFoundException("Salle de réunion introuvable."));
        MeetingRoomService service = request.serviceId() == null ? line.getMeetingRoomService() : meetingRoomServiceRepository.findById(request.serviceId()).orElseThrow(() -> new ResourceNotFoundException("Formule de salle de réunion introuvable."));
        LocalDate serviceDate = request.serviceDate() == null ? line.getServiceDate() : request.serviceDate();
        if (serviceDate.isBefore(LocalDate.now(clock.withZone(MADAGASCAR)))) throw new ValidationException("La date de service ne peut pas être dans le passé.", "serviceDate");
        Integer guests = request.guestsCount() == null ? line.getGuestsCount() : request.guestsCount();
        if (guests != null && guests < 1) throw new ValidationException("Le nombre de personnes doit être positif.", "guestsCount");
        if (room.getCapacity() != null && guests != null && guests > room.getCapacity()) throw new UnprocessableEntityException("VALIDATION", "La capacité de la salle est insuffisante.", "guestsCount");
        if (bookingMeetingRoomRepository.existsActiveBookingForRoomAndDateExcluding(room.getIdMeetingRoom(), serviceDate, id)) throw new ConflictException("MEETING_ROOM_UNAVAILABLE", "La salle est déjà réservée pour cette date.");
        boolean reprice = request.meetingRoomId() != null || request.serviceId() != null || request.serviceDate() != null;
        MeetingRoomRate rate = null;
        if (reprice) { rate = selectRate(meetingRoomRateRepository.findApplicableForBooking(room.getIdMeetingRoom(), service.getIdService(), serviceDate)); if (rate == null) throw new UnprocessableEntityException("NO_TARIFF", "Aucun tarif applicable pour cette salle.", "serviceId"); }
        int quantity = reprice ? quantityFor(rate, guests, "guestsCount") : line.getQuantity();
        BigDecimal oldTotal = bookingBalanceRepository.findById(booking.getIdBooking()).orElseThrow().getTotal();
        java.util.UUID operation = changeLogger.beginOperation();
        try {
            logMeeting(booking, line, "meeting_room_id", line.getMeetingRoom().getIdMeetingRoom(), room.getIdMeetingRoom(), request.reason(), user, operation);
            logMeeting(booking, line, "meeting_room_service_id", line.getMeetingRoomService().getIdService(), service.getIdService(), request.reason(), user, operation);
            logMeeting(booking, line, "service_date", line.getServiceDate(), serviceDate, request.reason(), user, operation);
            logMeeting(booking, line, "guests_count", line.getGuestsCount(), guests, request.reason(), user, operation);
            if (reprice) logMeeting(booking, line, "unit_price", line.getUnitPrice(), rate.getAmount(), request.reason(), user, operation);
        } finally { changeLogger.endOperation(); }
        line.setMeetingRoom(room); line.setMeetingRoomService(service); line.setServiceDate(serviceDate); line.setGuestsCount(guests); line.setQuantity(quantity); if (reprice) line.setUnitPrice(rate.getAmount());
        bookingMeetingRoomRepository.saveAndFlush(line);
        BigDecimal newTotal = bookingBalanceRepository.findById(booking.getIdBooking()).orElseThrow().getTotal();
        if (request.expectedNewTotal() != null && request.expectedNewTotal().compareTo(newTotal) != 0) throw new ConflictException("TOTAL_CHANGED", "Le total recalculé a changé.", "expectedNewTotal", newTotal);
        BigDecimal delta = newTotal.subtract(oldTotal);
        return details.get(booking.getIdBooking()).withAdjustment(delta, delta.signum() >= 0 ? "SUPPLEMENT" : "ECONOMIE");
    }

    private void logMeeting(Booking booking, BookingMeetingRoom line, String field, Object oldValue, Object newValue, String reason, User user, java.util.UUID operation) {
        if (!java.util.Objects.equals(oldValue, newValue)) changeLogger.log(booking.getIdBooking(), "MEETING_ROOM", line.getIdBookingMeetingRoom(), "UPDATE", field, oldValue, newValue, reason, user.getIdUser(), operation);
    }

    @Transactional
    public BookingDetailResponse createBooking(BookingMeetingRoomRequest request, String username) {
        permissions.require(username, null, "BOOKING_UPDATE");
        if (request != null && Boolean.TRUE.equals(request.confirm())) permissions.require(username, null, "BOOKING_CONFIRM");
        validateRequest(request);
        Set<String> uniqueLines = new HashSet<>();
        List<ResolvedMeetingLine> resolvedLines = new java.util.ArrayList<>();
        for (int index = 0; index < request.meetingRooms().size(); index++) {
            BookingMeetingRoomRequest.MeetingRoomItem item = request.meetingRooms().get(index);
            String field = "meetingRooms[" + index + "]";
            String key = item.meetingRoomId() + ":" + item.serviceDate();
            if (!uniqueLines.add(key))
                throw new ValidationException("Cette salle est déjà ajoutée pour cette date.", field + ".meetingRoomId");
            if (item.serviceDate().isBefore(LocalDate.now(clock)))
                throw new ValidationException("La date de service ne peut pas être dans le passé.", field + ".serviceDate");
            MeetingRoom room = meetingRoomRepository.findById(item.meetingRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salle de réunion introuvable."));
            MeetingRoomService service = meetingRoomServiceRepository.findById(item.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formule de salle de réunion introuvable."));
            if (room.getCapacity() != null && item.guestsCount() != null && item.guestsCount() > room.getCapacity())
                throw new UnprocessableEntityException("VALIDATION", "La capacité de la salle " + room.getName() + " est insuffisante.", field + ".guestsCount");
            if (bookingMeetingRoomRepository.existsActiveBookingForRoomAndDate(room.getIdMeetingRoom(), item.serviceDate()))
                throw new ConflictException("MEETING_ROOM_UNAVAILABLE",
                        "La salle " + room.getName() + " est déjà réservée le " + item.serviceDate() + ".",
                        field + ".meetingRoomId", null);
            List<MeetingRoomRate> rates = meetingRoomRateRepository.findApplicableForBooking(
                    room.getIdMeetingRoom(), service.getIdService(), item.serviceDate());
            if (rates.isEmpty())
                throw new UnprocessableEntityException("NO_TARIFF", "Aucun tarif applicable pour " + room.getName() + " le " + item.serviceDate() + ".", field + ".serviceId");
            MeetingRoomRate rate = selectRate(rates);
            if (rate == null)
                throw new UnprocessableEntityException("NO_TARIFF", "Aucun tarif unique applicable pour " + room.getName() + ".", field + ".serviceId");
            int quantity = quantityFor(rate, item.guestsCount(), field + ".guestsCount");
            resolvedLines.add(new ResolvedMeetingLine(item, room, service, rate, quantity));
        }

        Customer customer = common.handleCustomer(request.customerId(), request.newCustomer());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        BookingStatus finalStatus = common.findBookingStatus(Boolean.TRUE.equals(request.confirm()) ? "CONFIRMED" : "PENDING");
        BookingLineStatus confirmedLine = common.findLineStatus("CONFIRMED");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(request.origin() == null ? mg.lagrace.api.booking.core.BookingOrigin.LOCAL : request.origin());
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(finalStatus);
        booking = bookingRepository.saveAndFlush(booking);

        for (ResolvedMeetingLine resolved : resolvedLines) {
            BookingMeetingRoomRequest.MeetingRoomItem item = resolved.item();
            BookingMeetingRoom line = new BookingMeetingRoom();
            line.setBooking(booking);
            line.setMeetingRoom(resolved.room());
            line.setMeetingRoomService(resolved.service());
            line.setServiceDate(item.serviceDate());
            line.setGuestsCount(item.guestsCount());
            line.setQuantity(resolved.quantity());
            line.setUnitPrice(resolved.rate().getAmount());
            line.setIncluded(false);
            line.setStatus(confirmedLine);
            line.setActive(true);
            line = bookingMeetingRoomRepository.saveAndFlush(line);
            common.saveHistory(booking, "MEETING_ROOM", line.getIdBookingMeetingRoom(), confirmedLine.getCode(), user);
        }

        LocalDate extrasRateDate = request.meetingRooms().stream().map(BookingMeetingRoomRequest.MeetingRoomItem::serviceDate)
                .min(LocalDate::compareTo).orElseThrow();
        extraService.addToBooking(booking, request.extras(), user, extrasRateDate, "MEETING_ROOM");
        BigDecimal total = readTotal(booking.getIdBooking(), request.expectedTotal());
        common.saveHistory(booking, null, null, finalStatus.getCode(), user);
        return details.get(booking.getIdBooking());
    }

    private BigDecimal readTotal(Long bookingId, BigDecimal expectedTotal) {
        BookingBalance balance = bookingBalanceRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."));
        BigDecimal total = balance.getTotal();
        if (expectedTotal != null && expectedTotal.compareTo(total) != 0)
            throw new ConflictException("TOTAL_CHANGED", "Le total calculé est " + total + " Ar. Vérifiez le récapitulatif.", "expectedTotal", total);
        return total;
    }

    private MeetingRoomRate selectRate(List<MeetingRoomRate> rates) {
        MeetingRoomRate packageRate = rates.stream().filter(rate -> rate.getUnit() != null
                && "forfait".equalsIgnoreCase(rate.getUnit().getName())).findFirst().orElse(null);
        if (packageRate != null) return packageRate;
        MeetingRoomRate dayRate = rates.stream().filter(rate -> rate.getUnit() != null
                && "jour".equalsIgnoreCase(rate.getUnit().getName())).findFirst().orElse(null);
        return dayRate != null ? dayRate : rates.size() == 1 ? rates.get(0) : null;
    }

    private int quantityFor(MeetingRoomRate rate, Integer guests, String field) {
        String unit = rate.getUnit() == null || rate.getUnit().getName() == null
                ? "" : rate.getUnit().getName().trim();
        if ("forfait".equalsIgnoreCase(unit) || "jour".equalsIgnoreCase(unit)) return 1;
        if ("par personne".equalsIgnoreCase(unit)) {
            if (guests == null || guests < 1)
                throw new ValidationException("Le nombre de personnes est obligatoire pour ce tarif.", field);
            return guests;
        }
        return 1;
    }

    private record ResolvedMeetingLine(BookingMeetingRoomRequest.MeetingRoomItem item,
            MeetingRoom room, MeetingRoomService service, MeetingRoomRate rate, int quantity) {}

    private void validateRequest(BookingMeetingRoomRequest request) {
        if (request == null || (request.customerId() == null) == (request.newCustomer() == null))
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        if (request.meetingRooms() == null || request.meetingRooms().isEmpty())
            throw new ValidationException("Au moins une salle de réunion est requise.");
    }
}
