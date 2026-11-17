package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.booking.BookingDetailResponse.*;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.*;
import mg.lagrace.api.models.views.BookingBalance;
import mg.lagrace.api.models.views.BookingLineView;
import mg.lagrace.api.repositories.*;

@Service
public class BookingDetailService {
    private final BookingRepository bookings; private final BookingBalanceRepository balances;
    private final BookingLineViewRepository lines; private final BookingChargeRepository charges;
    private final BookingOtherRepository others; private final PaymentRepository payments;
    private final BookingStatusHistoryRepository history;
        private final BookingLineStatusRepository lineStatuses;
        private final RoomRepository rooms;
        private final PoolRepository pools;
        private final MeetingRoomRepository meetingRooms;
        private final SpaceRepository spaces;
        private final BookingSpaceRepository bookingSpaces;
        private final AdditionalServiceRepository additionalServices;
        private final BookingPoolRepository bookingPools;
        private final BookingMeetingRoomRepository bookingMeetingRooms;
        private final BookingInvoicingService invoicing;

    public BookingDetailService(BookingRepository bookings, BookingBalanceRepository balances,
            BookingLineViewRepository lines, BookingChargeRepository charges, BookingOtherRepository others,
                        PaymentRepository payments, BookingStatusHistoryRepository history, BookingLineStatusRepository lineStatuses,
                        RoomRepository rooms, PoolRepository pools, MeetingRoomRepository meetingRooms, SpaceRepository spaces,
                        BookingSpaceRepository bookingSpaces, AdditionalServiceRepository additionalServices,
                        BookingPoolRepository bookingPools, BookingMeetingRoomRepository bookingMeetingRooms,
                        BookingInvoicingService invoicing) {
        this.bookings = bookings; this.balances = balances; this.lines = lines; this.charges = charges;
                this.others = others; this.payments = payments; this.history = history; this.lineStatuses = lineStatuses;
                this.rooms = rooms; this.pools = pools; this.meetingRooms = meetingRooms; this.spaces = spaces;
                this.bookingSpaces = bookingSpaces; this.additionalServices = additionalServices;
        this.bookingPools = bookingPools; this.bookingMeetingRooms = bookingMeetingRooms;
        this.invoicing = invoicing;
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse get(Long id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> notFound(id));
        return build(booking);
    }

        @Transactional(readOnly = true)
        public BookingDetailResponse.Invoicing getInvoicing(Long id) {
                return invoicing.get(id);
        }

    @Transactional(readOnly = true)
    public BookingDetailResponse getByRef(String ref) {
        Booking booking = bookings.findByRef(ref).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable : " + ref));
        return build(booking);
    }

    private BookingDetailResponse build(Booking booking) {
        BookingBalance balance = balances.findById(booking.getIdBooking()).orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        boolean open = !Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode());
        List<BookingLineView> sourceLines = lines.findAllForBookings(List.of(booking.getIdBooking()));
        Map<Long, BookingPool> poolLines = bookingPools.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .collect(java.util.stream.Collectors.toMap(BookingPool::getIdBookingPool, line -> line));
        Map<Long, BookingMeetingRoom> meetingRoomLines = bookingMeetingRooms.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .collect(java.util.stream.Collectors.toMap(BookingMeetingRoom::getIdBookingMeetingRoom, line -> line));
        BookingLineView mainLine = selectMainLine(sourceLines, poolLines);
        Map<Long, BookingSpace> spaceLines = bookingSpaces.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .collect(java.util.stream.Collectors.toMap(BookingSpace::getIdBookingSpace, line -> line));
        List<BookingDetailResponse.Line> detailLines = sourceLines.stream()
                .map(line -> line(line, open, line == mainLine, spaceLines.get(line.getId().getLineId()),
                        poolLines.get(line.getId().getLineId()), meetingRoomLines.get(line.getId().getLineId()),
                        booking.getStatus().getCode()))
                .toList();
        boolean canCancelBooking = open && detailLines.stream()
                .noneMatch(line -> Set.of("CHECK_IN", "CHECK_OUT").contains(line.statusCode()));
        List<BookingCharge> sourceCharges = charges.findAllByBooking_IdBooking(booking.getIdBooking());
        List<BookingDetailResponse.Charge> detailCharges = sourceCharges.stream()
                .map(c -> new Charge(c.getIdBookingCharge(), c.getVersion(), c.getLabel(), c.getQuantity(), c.getUnitPrice(),
                        c.getIncluded(), c.getDeposit(), c.getAmount(), c.getVoidedAt() != null, c.getVoidReason(),
                        category(c), c.getAdditionalService() == null || c.getAdditionalService().getScope() == null
                                ? null : c.getAdditionalService().getScope().getCode(),
                        c.getAdditionalService() != null && c.getAdditionalService().isFreeWithCaterer(),
                        c.getBanquetRate() == null ? null : c.getBanquetRate().getMenuType().getIdMenuType(),
                        c.getBanquetRate() == null ? null : c.getBanquetRate().getMinPersons(),
                        c.getBanquetRate() == null ? null : c.getBanquetRate().getMaxPersons(),
                        new AllowedItem(open, open))).toList();
        List<BookingDetailResponse.Other> detailOthers = others.findAllByBooking_IdBookingOrderByCreatedAtDesc(booking.getIdBooking()).stream()
                .map(o -> new Other(o.getIdBookingOther(), o.getVersion(), o.getLabel(), o.getAmount(),
                        Boolean.TRUE.equals(o.getIsCancellationFee()), o.getVoidedAt() != null,
                        o.getVoidReason(), new AllowedItem(open, open))).toList();
        List<mg.lagrace.api.models.Payment> sourcePayments = payments.findAllByBooking_IdBooking(booking.getIdBooking());
        List<BookingDetailResponse.Payment> detailPayments = sourcePayments.stream()
                .map(p -> new BookingDetailResponse.Payment(p.getIdPayment(), p.getKind().getName(), p.getKind().getCode(),
                        p.getMethod().getName(), p.getAmount(), p.getPaidAt(), p.getNote())).toList();
        List<History> detailHistory = history.findTop30ByBooking_IdBookingOrderByChangedAtDesc(booking.getIdBooking()).stream()
                .map(h -> new History(h.getIdHistory(), h.getLineType(), h.getLineId(), h.getOldStatusCode(), h.getNewStatusCode(),
                        h.getReason(), h.getChangedAt(), h.getChangedByUser() == null ? null : h.getChangedByUser().getUsername())).toList();
        BigDecimal remaining = n(balance.getBalance());
        boolean hasSpace = !spaceLines.isEmpty();
        DepositSummary depositSummary = hasSpace ? depositSummary(spaceLines.values().stream().toList(), sourceCharges,
                sourcePayments, balance, booking.getStatus().getCode(), open) : null;
        long unfinishedCount = detailLines.stream().filter(l -> !Set.of("CHECK_OUT", "CANCELED").contains(l.statusCode())).count();
        List<String> blockers = new java.util.ArrayList<>();
        if (remaining.signum() != 0) blockers.add("solde restant de " + remaining.stripTrailingZeros().toPlainString() + " Ar");
                if (hasSpace && depositSummary != null && depositSummary.deposit().held().signum() > 0)
                        blockers.add("caution non rendue");
        if (unfinishedCount > 0) blockers.add(unfinishedCount + " ligne(s) non terminée(s)");
        Allowed allowed = new Allowed(open, open, open && "PENDING".equals(booking.getStatus().getCode()),
                open && "IN_PROGRESS".equals(booking.getStatus().getCode()) && blockers.isEmpty(), canCancelBooking,
                open, true, true);
        return new BookingDetailResponse(booking.getIdBooking(), booking.getRef(), booking.getVersion(), booking.getStatus().getName(), booking.getStatus().getCode(),
                new BookingDetailResponse.Customer(booking.getCustomer().getIdCustomer(), booking.getCustomer().getFirstName(), booking.getCustomer().getLastName(),
                        booking.getCustomer().getPhone(), booking.getCustomer().getEmail()),
                booking.getCreatedByUser() == null ? null : new Creator(booking.getCreatedByUser().getIdUser(), booking.getCreatedByUser().getUsername(),
                        booking.getCreatedByUser().getFirstName(), booking.getCreatedByUser().getLastName()), booking.getOrigin(), booking.getNotes(), detailLines,
                detailCharges, detailOthers, detailPayments, detailHistory,
                new Balance(n(balance.getLinesTotal()), n(balance.getChargesTotal()), n(balance.getOthersTotal()), n(balance.getTotal()),
                        n(balance.getPaid()), n(balance.getBalance()), hasSpace ? n(balance.getDepositDue()) : null,
                        hasSpace ? n(balance.getDepositHeld()) : null),
                allowed, blockers.isEmpty(), blockers, null, null, null, null, null,
                depositSummary == null ? null : depositSummary.deposit(), null)
                .withInvoicing(invoicing.get(booking.getIdBooking()));
    }

    private Line line(BookingLineView source, boolean open, boolean main, BookingSpace spaceLine,
            BookingPool poolLine, BookingMeetingRoom meetingRoomLine, String bookingStatus) {
        String status = source.getStatusCode();
        boolean editable = open && "CONFIRMED".equals(status);
        boolean poolPrivate = poolLine != null && Boolean.TRUE.equals(poolLine.getPoolService().getIsExculusive());
        AllowedLine flags = spaceLine != null
                ? new AllowedLine(editable, null, null, null,
                        open && Set.of("PENDING", "CONFIRMED").contains(bookingStatus) && "CONFIRMED".equals(status),
                        open && "CHECK_IN".equals(status), editable)
                : source.getId().getLineType().equals("ROOM")
                        ? new AllowedLine(open, open && Set.of("CONFIRMED", "CHECK_IN").contains(status),
                                open && "CONFIRMED".equals(status), open && "CHECK_IN".equals(status), null, null,
                                editable)
                        : new AllowedLine(false, poolPrivate && open && Set.of("CONFIRMED", "CHECK_IN").contains(status), null, null,
                                open && "CONFIRMED".equals(status), open && "CHECK_IN".equals(status), editable);
        SpaceEvent eventType = spaceLine == null ? null : spaceLine.getEventType();
        Unit unit = spaceLine == null ? null : spaceLine.getUnit();
        PoolRate poolRate = poolLine == null ? null : poolLine.getPoolRate();
        MeetingRoom meetingRoom = meetingRoomLine == null ? null : meetingRoomLine.getMeetingRoom();
        MeetingRoomService meetingService = meetingRoomLine == null ? null : meetingRoomLine.getMeetingRoomService();
        return new Line(source.getId().getLineType(), source.getId().getLineId(), source.getResourceId(), source.getReference(), source.getVersion(),
                                resourceLabel(source), source.getId().getLineType().equals("ROOM") ? source.getStartsAt().toLocalDate() : null,
                source.getId().getLineType().equals("ROOM") && source.getEndsAt() != null ? source.getEndsAt().toLocalDate() : null,
                source.getQuantity(), source.getUnitPrice(), source.getIncluded(), source.getAmount(), statusName(status), status,
                flags, main, spaceLine == null ? null : spaceLine.getSpace().getName(),
                spaceLine == null ? null : spaceLine.getEventDate(), eventType == null ? null : eventType.getIdEvent(),
                eventType == null ? null : eventType.getName(),
                spaceLine == null ? null : spaceLine.getGuestsCount(), spaceLine == null ? null : spaceLine.getWithCaterer(),
                unit == null ? null : unit.getName(), poolLine == null ? null : poolLine.getPool().getName(),
                meetingRoom == null ? null : meetingRoom.getName(),
                poolLine != null ? poolLine.getPoolService().getName() : meetingService == null ? null : meetingService.getName(),
                poolLine != null ? poolLine.getPoolService().getCode() : meetingService == null ? null : meetingService.getCode(),
                poolLine == null ? null : poolLine.getStartsAt(), poolLine == null ? null : poolLine.getEndsAt(),
                meetingRoomLine == null ? null : meetingRoomLine.getServiceDate(),
                poolRate == null ? null : poolRate.getIsMealIncluded(),
                poolRate == null ? null : poolRate.getAgeLabel(), poolLine == null ? null : poolPrivate,
                source.getQuantity());
    }

    private ChargeCategory category(BookingCharge charge) {
        if (charge.getBanquetRate() != null) return ChargeCategory.BANQUET;
        if (Boolean.TRUE.equals(charge.getDeposit())) return ChargeCategory.CAUTION;
        return charge.getAdditionalService() != null ? ChargeCategory.SERVICE : ChargeCategory.MENU;
    }

    private DepositSummary depositSummary(List<BookingSpace> spaceLines, List<BookingCharge> sourceCharges,
            List<mg.lagrace.api.models.Payment> sourcePayments, BookingBalance balance, String bookingStatus, boolean open) {
        BookingCharge collectedCharge = sourceCharges.stream()
                .filter(charge -> Boolean.TRUE.equals(charge.getDeposit())
                        && charge.getAdditionalService() != null
                        && "SPACE".equalsIgnoreCase(charge.getAdditionalService().getScope().getCode()))
                .findFirst().orElse(null);
        BigDecimal amount = collectedCharge == null ? additionalServices.findActiveSpaceDeposits().stream()
                .findFirst().map(AdditionalService::getPrice)
                .orElseGet(() -> sourceCharges.stream().filter(charge -> Boolean.TRUE.equals(charge.getDeposit()))
                        .map(BookingCharge::getUnitPrice).findFirst().orElse(BigDecimal.ZERO))
                : collectedCharge.getUnitPrice().multiply(BigDecimal.valueOf(collectedCharge.getQuantity()));
        BigDecimal held = n(balance.getDepositHeld());
        BigDecimal returned = sourcePayments.stream().filter(payment -> "DEPOSIT_REFUND".equals(payment.getKind().getCode()))
                .map(mg.lagrace.api.models.Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retained = sourcePayments.stream().filter(payment -> "PAYMENT".equals(payment.getKind().getCode())
                && "Prélevé sur la caution".equals(payment.getNote()))
                .map(mg.lagrace.api.models.Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasCollectedCharge = collectedCharge != null || sourcePayments.stream()
                .anyMatch(payment -> "DEPOSIT".equals(payment.getKind().getCode()));
        String state = held.signum() > 0
                ? (held.compareTo(amount) < 0 ? "PARTIALLY_RETAINED" : "HELD")
                : returned.signum() > 0 ? retained.signum() > 0 ? "PARTIALLY_RETAINED" : "RETURNED"
                        : "NOT_COLLECTED";
        boolean canceled = "CANCELED".equals(bookingStatus);
        boolean eventEnded = !spaceLines.isEmpty() && spaceLines.stream()
                .filter(line -> Boolean.TRUE.equals(line.getActive()) || "CHECK_OUT".equals(code(line.getStatus())))
                .allMatch(line -> "CHECK_OUT".equals(code(line.getStatus())));
        boolean activeSpace = spaceLines.stream().anyMatch(line -> Boolean.TRUE.equals(line.getActive()));
        BookingDetailResponse.Deposit deposit = new BookingDetailResponse.Deposit(amount, state, held,
                hasCollectedCharge ? BigDecimal.ZERO : amount, held,
                new BookingDetailResponse.DepositAllowed(open && activeSpace && !hasCollectedCharge,
                        held.signum() > 0 && (eventEnded || canceled)));
        return new DepositSummary(deposit);
    }

    private String code(BookingLineStatus status) { return status == null ? null : status.getCode(); }
    private record DepositSummary(BookingDetailResponse.Deposit deposit) {}

    private BookingLineView selectMainLine(List<BookingLineView> sourceLines, Map<Long, BookingPool> poolLines) {
                return sourceLines.stream()
                                .min(java.util.Comparator.comparingInt((BookingLineView line) -> switch (line.getId().getLineType()) {
                                        case "SPACE" -> 0;
                                        case "MEETING_ROOM" -> 1;
                                        case "POOL" -> Boolean.TRUE.equals(poolLines.get(line.getId().getLineId())
                                                .getPoolService().getIsExculusive()) ? 2 : 4;
                                        case "ROOM" -> 3;
                                        default -> 5;
                                }).thenComparing(BookingLineView::getStartsAt,
                                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                                                .thenComparing(line -> line.getId().getLineId()))
                                .orElse(null);
        }

        private String statusName(String code) {
                BookingLineStatus status = lineStatuses.findByCode(code);
                return status == null ? code : status.getName();
        }

        private String resourceLabel(BookingLineView source) {
                String type = source.getId().getLineType();
                Long id = source.getResourceId();
                return switch (type) {
                        case "ROOM" -> rooms.findById(id).map(room -> "Chambre " + room.getRefRoom()).orElse(source.getReference());
                        case "POOL" -> pools.findById(id).map(Pool::getName).orElse(source.getReference());
                        case "MEETING_ROOM" -> meetingRooms.findById(id).map(MeetingRoom::getName).orElse(source.getReference());
                        case "SPACE" -> spaces.findById(id).map(Space::getName).orElse(source.getReference());
                        default -> source.getReference();
                };
        }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private ResourceNotFoundException notFound(Long id) { return new ResourceNotFoundException("Dossier introuvable avec l'id : " + id); }
}
