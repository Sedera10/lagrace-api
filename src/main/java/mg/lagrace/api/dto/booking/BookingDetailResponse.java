package mg.lagrace.api.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import mg.lagrace.api.models.enums.BookingOrigin;

public record BookingDetailResponse(Long id, String ref, Integer version, String status,
        Customer customer, Creator creator, BookingOrigin origin, String notes, List<Line> lines, List<Charge> charges,
        List<Other> others, List<Payment> payments, List<History> history, Balance balance,
        Allowed allowed, boolean readyToClose, List<String> closeBlockers,
        BigDecimal adjustmentAmount, String adjustmentType, Boolean bookingHasNoActiveLine,
        BigDecimal refundDue, BigDecimal depositToReturn) {
                public BookingDetailResponse withAdjustment(BigDecimal amount, String type) {
                        return new BookingDetailResponse(id, ref, version, status, customer, creator, origin, notes, lines, charges,
                                    others, payments, history, balance, allowed, readyToClose, closeBlockers, amount, type,
                                    bookingHasNoActiveLine, refundDue, depositToReturn);
                }

        public BookingDetailResponse(Long id, String ref, Integer version, String status,
                        Customer customer, Creator creator, BookingOrigin origin, String notes, List<Line> lines, List<Charge> charges,
                        List<Other> others, List<Payment> payments, List<History> history, Balance balance,
                        Allowed allowed, boolean readyToClose, List<String> closeBlockers) {
                this(id, ref, version, status, customer, creator, origin, notes, lines, charges, others, payments, history,
                                        balance, allowed, readyToClose, closeBlockers, null, null, null, null, null);
        }

            public BookingDetailResponse withCancellationSummary(Boolean noActiveLine, BigDecimal refund, BigDecimal deposit) {
                return new BookingDetailResponse(id, ref, version, status, customer, creator, origin, notes, lines, charges,
                        others, payments, history, balance, allowed, readyToClose, closeBlockers, adjustmentAmount,
                        adjustmentType, noActiveLine, refund, deposit);
            }

    public record Customer(Long id, String firstName, String lastName, String phone, String email) {}
    public record Creator(Long id, String username, String firstName, String lastName) {}
    public record Allowed(boolean edit, boolean addService, boolean confirm, boolean close, boolean cancel,
            boolean pay, boolean print, boolean invoice) {}
    public record Line(String type, Long id, String ref, Integer version, String resourceLabel,
            LocalDate checkIn, LocalDate checkOut, Integer nights, BigDecimal unitPrice, Boolean included,
            BigDecimal amount, String status, AllowedLine allowed) {}
    public record AllowedLine(boolean edit, boolean extend, boolean checkIn, boolean checkOut, boolean cancel) {}
    public record Charge(Long id, Integer version, String label, Integer quantity, BigDecimal unitPrice,
            Boolean included, Boolean deposit, BigDecimal amount, boolean voided, String voidReason, AllowedItem allowed) {}
    public record Other(Long id, Integer version, String label, BigDecimal amount, boolean voided,
            String voidReason, AllowedItem allowed) {}
    public record AllowedItem(boolean edit, @JsonProperty("void") boolean voidAllowed) {}
    public record Payment(Long id, String kind, String method, BigDecimal amount, OffsetDateTime paidAt, String note) {}
    public record History(Long id, String lineType, String lineId, String oldStatus, String newStatus,
            String reason, OffsetDateTime changedAt, String user) {}
    public record Balance(BigDecimal linesTotal, BigDecimal chargesTotal, BigDecimal othersTotal,
            BigDecimal total, BigDecimal paid, BigDecimal balance, BigDecimal depositDue, BigDecimal depositHeld) {}

}
