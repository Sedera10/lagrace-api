package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.booking.meeting.BookingMeetingRoomRequest;
import mg.lagrace.api.dto.booking.room.BookingResponse;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.exceptions.ConflictException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.UnprocessableEntityException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.Booking;
import mg.lagrace.api.models.BookingCharge;
import mg.lagrace.api.models.BookingLineStatus;
import mg.lagrace.api.models.BookingMeetingRoom;
import mg.lagrace.api.models.BookingStatus;
import mg.lagrace.api.models.Customer;
import mg.lagrace.api.models.MeetingRoom;
import mg.lagrace.api.models.MeetingRoomRate;
import mg.lagrace.api.models.MeetingRoomService;
import mg.lagrace.api.models.MenuItem;
import mg.lagrace.api.models.MenuItemRate;
import mg.lagrace.api.models.Payment;
import mg.lagrace.api.models.PaymentMethod;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.BookingBalanceRepository;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BookingMeetingRoomRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.MeetingRoomRateRepository;
import mg.lagrace.api.repositories.MeetingRoomRepository;
import mg.lagrace.api.repositories.MeetingRoomServiceRepository;
import mg.lagrace.api.repositories.MenuItemRateRepository;
import mg.lagrace.api.repositories.MenuItemRepository;
import mg.lagrace.api.repositories.PaymentKindRepository;
import mg.lagrace.api.repositories.PaymentMethodRepository;
import mg.lagrace.api.repositories.PaymentRepository;
import mg.lagrace.api.repositories.UserRepository;
import mg.lagrace.api.models.views.BookingBalance;

@Service
public class BookingMeetingRoomService {
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

    public BookingMeetingRoomService(BookingRoomService common, BookingRepository bookingRepository,
            BookingMeetingRoomRepository bookingMeetingRoomRepository, BookingChargeRepository bookingChargeRepository,
            BookingBalanceRepository bookingBalanceRepository, MeetingRoomRepository meetingRoomRepository,
            MeetingRoomServiceRepository meetingRoomServiceRepository, MeetingRoomRateRepository meetingRoomRateRepository,
            AdditionalServiceRepository additionalServiceRepository, MenuItemRepository menuItemRepository,
            MenuItemRateRepository menuItemRateRepository, PaymentMethodRepository paymentMethodRepository,
            PaymentKindRepository paymentKindRepository, PaymentRepository paymentRepository,
            UserRepository userRepository) {
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
    }

    @Transactional
    public BookingResponse createBooking(BookingMeetingRoomRequest request, String username) {
        validateRequest(request);
        Customer customer = common.handleCustomer(request.customerId(), request.newCustomer());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable."));
        BookingStatus pending = common.findBookingStatus("PENDING");
        BookingLineStatus confirmedLine = common.findLineStatus("CONFIRMED");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setOrigin(request.origin() == null ? mg.lagrace.api.models.enums.BookingOrigin.LOCAL : request.origin());
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setCreatedByUser(user);
        booking.setStatus(pending);
        booking = bookingRepository.saveAndFlush(booking);

        Set<String> uniqueLines = new HashSet<>();
        for (BookingMeetingRoomRequest.MeetingRoomItem item : request.meetingRooms()) {
            String key = item.meetingRoomId() + ":" + item.serviceDate();
            if (!uniqueLines.add(key)) throw new ValidationException("Une salle ne peut apparaître qu'une fois par jour.");
            if (item.serviceDate().isBefore(LocalDate.now(MADAGASCAR)))
                throw new ValidationException("La date de service ne peut pas être dans le passé.");
            MeetingRoom room = meetingRoomRepository.findById(item.meetingRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salle de réunion introuvable."));
            MeetingRoomService service = meetingRoomServiceRepository.findById(item.serviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Formule de salle de réunion introuvable."));
            if (room.getCapacity() != null && item.guestsCount() != null && item.guestsCount() > room.getCapacity())
                throw new UnprocessableEntityException("La capacité de la salle " + room.getName() + " est insuffisante.");
            if (bookingMeetingRoomRepository.existsActiveBookingForRoomAndDate(room.getIdMeetingRoom(), item.serviceDate()))
                throw new ConflictException("MEETING_ROOM_ALREADY_BOOKED", "La salle " + room.getName() + " est déjà réservée à cette date.");
            List<MeetingRoomRate> rates = meetingRoomRateRepository.findApplicableForBooking(
                    room.getIdMeetingRoom(), service.getIdService(), item.serviceDate());
                MeetingRoomRate rate = rates.stream()
                    .filter(candidate -> candidate.getUnit() != null
                        && "jour".equalsIgnoreCase(candidate.getUnit().getName()))
                    .findFirst()
                    .orElseGet(() -> rates.size() == 1 ? rates.get(0) : null);
                if (rate == null)
                throw new UnprocessableEntityException("Aucun tarif journalier unique pour la salle " + room.getName() + ".");
            BookingMeetingRoom line = new BookingMeetingRoom();
            line.setBooking(booking);
            line.setMeetingRoom(room);
            line.setMeetingRoomService(service);
            line.setServiceDate(item.serviceDate());
            line.setGuestsCount(item.guestsCount());
            line.setQuantity(1);
            line.setUnitPrice(rate.getAmount());
            line.setIncluded(false);
            line.setStatus(confirmedLine);
            line.setActive(true);
            line = bookingMeetingRoomRepository.saveAndFlush(line);
            common.saveHistory(booking, "MEETING_ROOM", line.getIdBookingMeetingRoom(), confirmedLine.getCode(), user);
        }

        LocalDate chargeDate = request.meetingRooms().get(0).serviceDate();
        createExtras(booking, request.extras(), user, chargeDate);
        PaymentMethod method = request.advance() == null ? null : paymentMethodRepository.findById(request.advance().methodId())
                .orElseThrow(() -> new ResourceNotFoundException("Moyen de paiement introuvable."));
        BigDecimal total = readTotal(booking.getIdBooking(), request.expectedTotal());
        if (request.advance() != null && request.advance().amount().compareTo(total) > 0)
            throw new UnprocessableEntityException("L'acompte ne peut pas dépasser le total de la réservation.");
        if (request.advance() != null) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setMethod(method);
            payment.setKind(paymentKindRepository.findByCode("ADVANCE")
                    .orElseThrow(() -> new ResourceNotFoundException("Type de paiement ADVANCE introuvable.")));
            payment.setAmount(request.advance().amount());
            payment.setReceivedByUser(user);
            paymentRepository.saveAndFlush(payment);
        }
        boolean confirmed = request.advance() != null || Boolean.TRUE.equals(request.confirm());
        BookingStatus finalStatus = common.findBookingStatus(confirmed ? "CONFIRMED" : "PENDING");
        booking.setStatus(finalStatus);
        bookingRepository.saveAndFlush(booking);
        common.saveHistory(booking, null, null, finalStatus.getCode(), user);
        return common.getBooking(booking.getIdBooking());
    }

    private BigDecimal readTotal(Long bookingId, BigDecimal expectedTotal) {
        BookingBalance balance = bookingBalanceRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("La vue v_booking_balance n'a retourné aucun résultat."));
        BigDecimal total = balance.getTotal().add(balance.getDepositDue());
        if (expectedTotal != null && expectedTotal.compareTo(total) != 0)
            throw new ConflictException("TOTAL_MISMATCH", "Le total calculé (" + total + ") ne correspond pas au total attendu (" + expectedTotal + ").");
        return total;
    }

    private void createExtras(Booking booking, List<ExtraItem> extras, User user, LocalDate date) {
        if (extras == null) return;
        for (ExtraItem extra : extras) {
            if (extra == null || extra.quantity() == null || extra.quantity() < 1
                    || (extra.menuItemId() == null) == (extra.additionalServiceId() == null))
                throw new ValidationException("Chaque extra doit avoir une quantité positive et une seule source.");
            BookingCharge charge = new BookingCharge();
            charge.setBooking(booking);
            charge.setQuantity(extra.quantity());
            charge.setIncluded(false);
            charge.setCreatedByUser(user);
            if (extra.additionalServiceId() != null) {
                AdditionalService service = additionalServiceRepository.findById(extra.additionalServiceId().longValue())
                        .orElseThrow(() -> new ResourceNotFoundException("Service additionnel introuvable."));
                if (!service.isActive() || service.getScope() == null
                        || !MEETING_SCOPE.equalsIgnoreCase(service.getScope().getCode()))
                    throw new UnprocessableEntityException("Le service additionnel ne s'applique pas à la salle de réunion.");
                charge.setAdditionalService(service);
                charge.setLabel(service.getName());
                charge.setUnitPrice(service.getPrice());
                charge.setDeposit(service.isDeposit());
            } else {
                MenuItem item = menuItemRepository.findById(extra.menuItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Article du menu introuvable."));
                if (!Boolean.TRUE.equals(item.getActive())) throw new UnprocessableEntityException("L'article du menu n'est pas actif.");
                MenuItemRate rate = menuItemRateRepository.findApplicableByMenuItem(item.getIdMenuItem(), date);
                if (rate == null) throw new UnprocessableEntityException("Aucun prix applicable pour l'article du menu.");
                charge.setMenuItem(item);
                charge.setLabel(item.getName());
                charge.setUnitPrice(rate.getAmount());
                charge.setDeposit(false);
            }
            bookingChargeRepository.saveAndFlush(charge);
        }
    }

    private void validateRequest(BookingMeetingRoomRequest request) {
        if (request == null || (request.customerId() == null) == (request.newCustomer() == null))
            throw new ValidationException("Indiquez exactement un client existant ou un nouveau client.");
        if (request.meetingRooms() == null || request.meetingRooms().isEmpty())
            throw new ValidationException("Au moins une salle de réunion est requise.");
        if (request.advance() != null && (request.advance().methodId() == null || request.advance().amount() == null
                || request.advance().amount().signum() <= 0))
            throw new ValidationException("Le moyen et le montant de l'acompte sont obligatoires.");
    }
}
