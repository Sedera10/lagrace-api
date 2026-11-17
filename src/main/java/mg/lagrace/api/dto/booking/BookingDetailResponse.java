package mg.lagrace.api.dto.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import mg.lagrace.api.models.enums.BookingOrigin;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingDetailResponse(Long id, String ref, Integer version, String status, String statusCode,
        Customer customer, Creator creator, BookingOrigin origin, String notes, List<Line> lines, List<Charge> charges,
        List<Other> others, List<Payment> payments, List<History> history, Balance balance,
        Allowed allowed, boolean readyToClose, List<String> closeBlockers,
        BigDecimal adjustmentAmount, String adjustmentType, Boolean bookingHasNoActiveLine,
                BigDecimal refundDue, BigDecimal depositToReturn, Deposit deposit, BigDecimal cashToHandBack,
                Invoicing invoicing) {
                public BookingDetailResponse withAdjustment(BigDecimal amount, String type) {
                        return new BookingDetailResponse(id, ref, version, status, statusCode, customer, creator, origin, notes, lines, charges,
                                    others, payments, history, balance, allowed, readyToClose, closeBlockers, amount, type,
                                    bookingHasNoActiveLine, refundDue, depositToReturn, deposit, cashToHandBack, invoicing);
                }

                public BookingDetailResponse(Long id, String ref, Integer version, String status, String statusCode,
                        Customer customer, Creator creator, BookingOrigin origin, String notes, List<Line> lines, List<Charge> charges,
                        List<Other> others, List<Payment> payments, List<History> history, Balance balance,
                        Allowed allowed, boolean readyToClose, List<String> closeBlockers,
                        BigDecimal adjustmentAmount, String adjustmentType, Boolean bookingHasNoActiveLine,
                        BigDecimal refundDue, BigDecimal depositToReturn, Deposit deposit, BigDecimal cashToHandBack) {
                    this(id, ref, version, status, statusCode, customer, creator, origin, notes, lines, charges,
                            others, payments, history, balance, allowed, readyToClose, closeBlockers,
                            adjustmentAmount, adjustmentType, bookingHasNoActiveLine, refundDue, depositToReturn,
                            deposit, cashToHandBack, null);
                }

                public BookingDetailResponse(Long id, String ref, Integer version, String status, String statusCode,
                        Customer customer, Creator creator, BookingOrigin origin, String notes, List<Line> lines, List<Charge> charges,
                        List<Other> others, List<Payment> payments, List<History> history, Balance balance,
                        Allowed allowed, boolean readyToClose, List<String> closeBlockers) {
                this(id, ref, version, status, statusCode, customer, creator, origin, notes, lines, charges, others, payments, history,
                                                balance, allowed, readyToClose, closeBlockers, null, null, null, null, null, null, null, null);
        }

            public BookingDetailResponse withCancellationSummary(Boolean noActiveLine, BigDecimal refund, BigDecimal deposit) {
                return new BookingDetailResponse(id, ref, version, status, statusCode, customer, creator, origin, notes, lines, charges,
                        others, payments, history, balance, allowed, readyToClose, closeBlockers, adjustmentAmount,
                                                adjustmentType, noActiveLine, refund, deposit, this.deposit, cashToHandBack, invoicing);
            }

                        public BookingDetailResponse withCashToHandBack(BigDecimal amount) {
                                return new BookingDetailResponse(id, ref, version, status, statusCode, customer, creator, origin, notes, lines, charges,
                                                others, payments, history, balance, allowed, readyToClose, closeBlockers, adjustmentAmount,
                                                adjustmentType, bookingHasNoActiveLine, refundDue, depositToReturn, deposit, amount, invoicing);
                        }

                        public BookingDetailResponse withInvoicing(Invoicing value) {
                                return new BookingDetailResponse(id, ref, version, status, statusCode, customer, creator, origin, notes,
                                        lines, charges, others, payments, history, balance, allowed, readyToClose, closeBlockers,
                                        adjustmentAmount, adjustmentType, bookingHasNoActiveLine, refundDue, depositToReturn,
                                        deposit, cashToHandBack, value);
                        }

    public record Customer(Long id, String firstName, String lastName, String phone, String email) {}
    public record Creator(Long id, String username, String firstName, String lastName) {}
    public record Deposit(BigDecimal amount, String state, BigDecimal held, BigDecimal toCollect,
            BigDecimal toReturn, DepositAllowed allowed) {}
        public record DepositAllowed(boolean collectDeposit, boolean returnDeposit) {}
    public record Allowed(boolean edit, boolean addService, boolean confirm, boolean close, boolean cancel,
            boolean pay, boolean print, boolean invoice) {}
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Line(String type, Long id, Long resourceId, String ref, Integer version, String resourceLabel,
            LocalDate checkIn, LocalDate checkOut, Integer nights, BigDecimal unitPrice, Boolean included,
                    BigDecimal amount, String status, String statusCode, AllowedLine allowed, boolean main,
                    String spaceLabel, LocalDate eventDate, Long eventTypeId, String eventTypeLabel, Integer guestsCount,
                                        Boolean withCaterer, String unitLabel, String poolLabel, String roomLabel, String formulaLabel,
                                        String formulaCode, OffsetDateTime startsAt, OffsetDateTime endsAt, LocalDate serviceDate,
                                            Boolean mealIncluded, String ageLabel, Boolean exclusive, Integer quantity) {}
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record AllowedLine(boolean edit, Boolean extend, Boolean checkIn, Boolean checkOut,
                    Boolean start, Boolean end, boolean cancel) {}
            public enum ChargeCategory { BANQUET, SERVICE, MENU, CAUTION }
    public record Charge(Long id, Integer version, String label, Integer quantity, BigDecimal unitPrice,
                    Boolean included, Boolean deposit, BigDecimal amount, boolean voided, String voidReason,
            ChargeCategory category, String scopeCode, boolean freeWithCaterer, Long menuTypeId, Integer rateMinPersons,
            Integer rateMaxPersons, AllowedItem allowed) {}
    public record Other(Long id, Integer version, String label, BigDecimal amount, boolean cancellationFee, boolean voided,
            String voidReason, AllowedItem allowed) {}
    public record AllowedItem(boolean edit, @JsonProperty("void") boolean voidAllowed) {}
        public record Payment(Long id, String kind, String kindCode, String method, BigDecimal amount, OffsetDateTime paidAt, String note) {}
    public record History(Long id, String lineType, String lineId, String oldStatus, String newStatus,
            String reason, OffsetDateTime changedAt, String user) {}
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Balance(BigDecimal linesTotal, BigDecimal chargesTotal, BigDecimal othersTotal,
            BigDecimal total, BigDecimal paid, BigDecimal balance, BigDecimal depositDue, BigDecimal depositHeld) {}
            public record Invoicing(BigDecimal unbilledAmount, long unbilledCount, List<InvoiceItem> invoices,
                    InvoicingAllowed allowed) {}
            public record InvoiceItem(Long id, String number, String type, BigDecimal total, BigDecimal netTotal,
                    String paymentStatus, String status, boolean outdated) {}
            public record InvoicingAllowed(boolean invoice, boolean credit) {}

}
