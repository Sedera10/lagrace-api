package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.booking.BookingDetailResponse;
import mg.lagrace.api.dto.invoice.BillableItemsResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.BookingCharge;
import mg.lagrace.api.models.BookingLineStatus;
import mg.lagrace.api.models.BookingMeetingRoom;
import mg.lagrace.api.models.BookingOther;
import mg.lagrace.api.models.BookingPool;
import mg.lagrace.api.models.BookingRoom;
import mg.lagrace.api.models.BookingSpace;
import mg.lagrace.api.models.Invoice;
import mg.lagrace.api.models.InvoiceLine;
import mg.lagrace.api.models.views.InvoiceSummary;
import mg.lagrace.api.repositories.BookingChargeRepository;
import mg.lagrace.api.repositories.BookingMeetingRoomRepository;
import mg.lagrace.api.repositories.BookingOtherRepository;
import mg.lagrace.api.repositories.BookingPoolRepository;
import mg.lagrace.api.repositories.BookingRepository;
import mg.lagrace.api.repositories.BookingRoomRepository;
import mg.lagrace.api.repositories.BookingSpaceRepository;
import mg.lagrace.api.repositories.InvoiceLineRepository;
import mg.lagrace.api.repositories.InvoiceRepository;
import mg.lagrace.api.repositories.InvoiceSummaryRepository;

@Service
@Transactional(readOnly = true)
public class BookingInvoicingService {
    private static final Set<String> INVOICE_ROLES = Set.of("ROLE_Receptionniste", "ROLE_Responsable", "ROLE_Administrateur");
    private static final Set<String> CREDIT_ROLES = Set.of("ROLE_Responsable", "ROLE_Administrateur");

    private final BookingRepository bookings;
    private final BillingReadService billingRead;
    private final InvoiceRepository invoices;
    private final InvoiceSummaryRepository summaries;
    private final InvoiceLineRepository invoiceLines;
    private final BookingRoomRepository rooms;
    private final BookingPoolRepository pools;
    private final BookingMeetingRoomRepository meetingRooms;
    private final BookingSpaceRepository spaces;
    private final BookingChargeRepository charges;
    private final BookingOtherRepository others;

    public BookingInvoicingService(BookingRepository bookings, BillingReadService billingRead,
            InvoiceRepository invoices, InvoiceSummaryRepository summaries, InvoiceLineRepository invoiceLines,
            BookingRoomRepository rooms, BookingPoolRepository pools, BookingMeetingRoomRepository meetingRooms,
            BookingSpaceRepository spaces, BookingChargeRepository charges, BookingOtherRepository others) {
        this.bookings = bookings;
        this.billingRead = billingRead;
        this.invoices = invoices;
        this.summaries = summaries;
        this.invoiceLines = invoiceLines;
        this.rooms = rooms;
        this.pools = pools;
        this.meetingRooms = meetingRooms;
        this.spaces = spaces;
        this.charges = charges;
        this.others = others;
    }

    public BookingDetailResponse.Invoicing get(Long bookingId) {
        bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable avec l'id : " + bookingId));
        BillableItemsResponse billable = billingRead.billableItems(bookingId);
        List<BillableItemsResponse.Item> unbilled = java.util.stream.Stream
                .concat(billable.items().stream(), billable.discountItems().stream()).toList();
        BigDecimal unbilledAmount = unbilled.stream().map(BillableItemsResponse.Item::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BookingDetailResponse.InvoiceItem> invoiceItems = invoices
                .findAllByBooking_IdBookingOrderByIssueDateDescIdInvoiceDesc(bookingId).stream()
                .map(this::invoiceItem).toList();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new BookingDetailResponse.Invoicing(unbilledAmount, unbilled.size(), invoiceItems,
                new BookingDetailResponse.InvoicingAllowed(!unbilled.isEmpty() && hasRole(authentication, INVOICE_ROLES),
                        invoiceItems.stream().anyMatch(item -> "INVOICE".equals(item.type())
                                && Set.of("ISSUED", "PARTIALLY_CREDITED").contains(item.status()) && item.netTotal().signum() > 0)
                                && hasRole(authentication, CREDIT_ROLES)));
    }

    private BookingDetailResponse.InvoiceItem invoiceItem(Invoice invoice) {
        InvoiceSummary summary = summaries.findById(invoice.getIdInvoice())
                .orElseThrow(() -> new IllegalStateException("Résumé de facture introuvable."));
        boolean creditNote = "CREDIT_NOTE".equals(invoice.getInvoiceType());
        BigDecimal sign = creditNote ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        List<InvoiceLine> lines = invoiceLines.findAllByInvoice_IdInvoiceOrderByPositionAsc(invoice.getIdInvoice());
        boolean outdated = lines.stream().anyMatch(this::outdated);
        return new BookingDetailResponse.InvoiceItem(invoice.getIdInvoice(), invoice.getInvoiceNumber(),
                invoice.getInvoiceType(), invoice.getTotal().multiply(sign),
                creditNote ? invoice.getTotal().negate() : value(summary.getNetTotal()),
                summary.getPaymentStatus(), invoice.getStatus().getCode(), outdated);
    }

    private boolean outdated(InvoiceLine line) {
        if (line.getSourceType() == null || line.getSourceId() == null) return false;
        return switch (line.getSourceType()) {
            case "ROOM" -> rooms.findById(line.getSourceId()).map(source ->
                    canceled(source.getStatus()) || changed(line, source.getAmount(), BigDecimal.valueOf(source.getNights()), effectivePrice(source.getIncluded(), source.getUnitPrice()))).orElse(true);
            case "POOL" -> pools.findById(line.getSourceId()).map(source ->
                    canceled(source.getStatus()) || changed(line, source.getAmount(), decimal(source.getQuantity()), effectivePrice(source.getIncluded(), source.getUnitPrice()))).orElse(true);
            case "MEETING_ROOM" -> meetingRooms.findById(line.getSourceId()).map(source ->
                    canceled(source.getStatus()) || changed(line, source.getAmount(), decimal(source.getQuantity()), effectivePrice(source.getIncluded(), source.getUnitPrice()))).orElse(true);
            case "SPACE" -> spaces.findById(line.getSourceId()).map(source ->
                    canceled(source.getStatus()) || changed(line, source.getAmount(), decimal(source.getQuantity()), effectivePrice(source.getIncluded(), source.getUnitPrice()))).orElse(true);
            case "CHARGE" -> charges.findById(line.getSourceId()).map(source ->
                    source.getVoidedAt() != null || Boolean.TRUE.equals(source.getDeposit())
                            || changed(line, source.getAmount(), decimal(source.getQuantity()), effectivePrice(source.getIncluded(), source.getUnitPrice()))).orElse(true);
            case "OTHER" -> others.findById(line.getSourceId()).map(source ->
                    source.getVoidedAt() != null || compare(line.getAmount(), source.getAmount()) != 0).orElse(true);
            default -> false;
        };
    }

    private boolean changed(InvoiceLine line, BigDecimal amount, BigDecimal quantity, BigDecimal unitPrice) {
        return compare(line.getAmount(), amount) != 0 || compare(line.getQuantity(), quantity) != 0
                || compare(line.getUnitPrice(), unitPrice) != 0;
    }

    private int compare(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) return first == second ? 0 : 1;
        return first.compareTo(second);
    }

    private BigDecimal decimal(Integer value) { return BigDecimal.valueOf(value == null ? 0 : value); }
        private BigDecimal effectivePrice(Boolean included, BigDecimal unitPrice) {
                return Boolean.TRUE.equals(included) ? BigDecimal.ZERO : value(unitPrice);
        }
    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean canceled(BookingLineStatus status) { return status != null && "CANCELED".equals(status.getCode()); }
    private boolean hasRole(Authentication authentication, Set<String> roles) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> roles.contains(authority.getAuthority()));
    }
}
