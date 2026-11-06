package mg.lagrace.api.services;

import java.math.BigDecimal;
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

    public BookingDetailService(BookingRepository bookings, BookingBalanceRepository balances,
            BookingLineViewRepository lines, BookingChargeRepository charges, BookingOtherRepository others,
            PaymentRepository payments, BookingStatusHistoryRepository history) {
        this.bookings = bookings; this.balances = balances; this.lines = lines; this.charges = charges;
        this.others = others; this.payments = payments; this.history = history;
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse get(Long id) {
        Booking booking = bookings.findById(id).orElseThrow(() -> notFound(id));
        return build(booking);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getByRef(String ref) {
        Booking booking = bookings.findByRef(ref).orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable : " + ref));
        return build(booking);
    }

    private BookingDetailResponse build(Booking booking) {
        BookingBalance balance = balances.findById(booking.getIdBooking()).orElseThrow(() -> new IllegalStateException("Solde introuvable."));
        boolean open = !Set.of("CLOSED", "CANCELED").contains(booking.getStatus().getCode());
        List<BookingDetailResponse.Line> detailLines = lines.findAllForBookings(List.of(booking.getIdBooking())).stream()
                .map(line -> line(line, open)).toList();
        boolean canCancelBooking = open && detailLines.stream()
                .noneMatch(line -> Set.of("CHECK_IN", "CHECK_OUT").contains(line.status()));
        List<BookingDetailResponse.Charge> detailCharges = charges.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .map(c -> new Charge(c.getIdBookingCharge(), c.getVersion(), c.getLabel(), c.getQuantity(), c.getUnitPrice(),
                        c.getIncluded(), c.getDeposit(), c.getAmount(), c.getVoidedAt() != null, c.getVoidReason(),
                        new AllowedItem(open, open))).toList();
        List<BookingDetailResponse.Other> detailOthers = others.findAllByBooking_IdBookingOrderByCreatedAtDesc(booking.getIdBooking()).stream()
                .map(o -> new Other(o.getIdBookingOther(), o.getVersion(), o.getLabel(), o.getAmount(), o.getVoidedAt() != null,
                        o.getVoidReason(), new AllowedItem(open, open))).toList();
        List<BookingDetailResponse.Payment> detailPayments = payments.findAllByBooking_IdBooking(booking.getIdBooking()).stream()
                .map(p -> new BookingDetailResponse.Payment(p.getIdPayment(), p.getKind().getCode(),
                        p.getMethod().getName(), p.getAmount(), p.getPaidAt(), p.getNote())).toList();
        List<History> detailHistory = history.findTop30ByBooking_IdBookingOrderByChangedAtDesc(booking.getIdBooking()).stream()
                .map(h -> new History(h.getIdHistory(), h.getLineType(), h.getLineId(), h.getOldStatusCode(), h.getNewStatusCode(),
                        h.getReason(), h.getChangedAt(), h.getChangedByUser() == null ? null : h.getChangedByUser().getUsername())).toList();
        BigDecimal remaining = n(balance.getBalance());
        boolean unfinished = detailLines.stream().anyMatch(l -> !Set.of("CHECK_OUT", "CANCELED").contains(l.status()));
        List<String> blockers = new java.util.ArrayList<>();
        if (remaining.signum() != 0) blockers.add("solde " + remaining.stripTrailingZeros().toPlainString() + " Ar");
        if (n(balance.getDepositHeld()).signum() != 0) blockers.add("caution détenue " + n(balance.getDepositHeld()).stripTrailingZeros().toPlainString() + " Ar");
        if (unfinished) blockers.add("ligne non terminée");
        Allowed allowed = new Allowed(open, open, open && "PENDING".equals(booking.getStatus().getCode()),
                open && "IN_PROGRESS".equals(booking.getStatus().getCode()) && blockers.isEmpty(), canCancelBooking,
                open, true, true);
        return new BookingDetailResponse(booking.getIdBooking(), booking.getRef(), booking.getVersion(), booking.getStatus().getCode(),
                new BookingDetailResponse.Customer(booking.getCustomer().getIdCustomer(), booking.getCustomer().getFirstName(), booking.getCustomer().getLastName(),
                        booking.getCustomer().getPhone(), booking.getCustomer().getEmail()),
                booking.getCreatedByUser() == null ? null : new Creator(booking.getCreatedByUser().getIdUser(), booking.getCreatedByUser().getUsername(),
                        booking.getCreatedByUser().getFirstName(), booking.getCreatedByUser().getLastName()), booking.getOrigin(), booking.getNotes(), detailLines,
                detailCharges, detailOthers, detailPayments, detailHistory,
                new Balance(n(balance.getLinesTotal()), n(balance.getChargesTotal()), n(balance.getOthersTotal()), n(balance.getTotal()),
                        n(balance.getPaid()), n(balance.getBalance()), n(balance.getDepositDue()), n(balance.getDepositHeld())),
                allowed, blockers.isEmpty(), blockers);
    }

    private Line line(BookingLineView source, boolean open) {
        String status = source.getStatusCode();
        AllowedLine flags = new AllowedLine(open, open && "CONFIRMED".equals(status),
                open && "CONFIRMED".equals(status), open && "CHECK_IN".equals(status),
                open && Set.of("CONFIRMED", "CHECK_IN").contains(status));
        return new Line(source.getId().getLineType(), source.getId().getLineId(), source.getReference(), source.getVersion(),
                source.getReference(), source.getId().getLineType().equals("ROOM") ? source.getStartsAt().toLocalDate() : null,
                source.getId().getLineType().equals("ROOM") && source.getEndsAt() != null ? source.getEndsAt().toLocalDate() : null,
                source.getQuantity(), source.getUnitPrice(), source.getIncluded(), source.getAmount(), status, flags);
    }
    private BigDecimal n(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private ResourceNotFoundException notFound(Long id) { return new ResourceNotFoundException("Dossier introuvable avec l'id : " + id); }
}
