package mg.lagrace.api.dashboard.dto.dashboard;

import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.room.RoomType;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.customer.Customer;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.payment.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Small, read-only projections returned by the dashboard endpoints. */
public final class DashboardDtos {
    private DashboardDtos() {}
    public record Count(long value) {}
    public record Rate(BigDecimal value) {}
    public record Money(BigDecimal value) {}
    public record Arrival(String reference, String customer, String room, LocalDate date, String status) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Activity(String label, BigDecimal amount, BigDecimal total) {
        public Activity(String label, BigDecimal amount) { this(label, amount, null); }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Comparison(BigDecimal value, BigDecimal previousValue) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SeriesPoint(String label, BigDecimal value, BigDecimal previousValue) {}
    public record Series(List<SeriesPoint> points) {}
    public record ActivityReport(Series series, Comparison total) {}
    public record RevenueByMonth(Series realized, Series invoiced, Series collected) {}
    public record OccupancyReport(Comparison total, Series series) {}
    public record CancellationReport(Comparison total, Series series) {}
    public record AverageStayReport(Comparison total, Series series) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RoomTypeRate(String roomType, BigDecimal rate, BigDecimal previousRate,
            long nightsReserved, long nightsAvailable) {
        public RoomTypeRate(String roomType, BigDecimal rate) { this(roomType, rate, null, 0, 0); }
        public RoomTypeRate(String roomType, BigDecimal rate, long nightsReserved, long nightsAvailable) {
            this(roomType, rate, null, nightsReserved, nightsAvailable);
        }
    }
    public record BookingReportRow(long bookingId, String reference, String customer, String origin, String status,
            LocalDate periodStart, LocalDate periodEnd, String services, BigDecimal total, BigDecimal paid,
            BigDecimal balance, java.time.OffsetDateTime createdAt) {}
    public record BookingTotals(long count, BigDecimal total, BigDecimal paid, BigDecimal balance) {}
    public record BookingsReport(List<BookingReportRow> items, BookingTotals totals, int page, int size) {}
    public record Progress(long total, long done) {}
    public record Occupancy(long occupied, long total, BigDecimal percent) {}
    public record Cards(Progress arrivals, Progress departures, Occupancy occupiedRooms, BigDecimal amountToCollect, BigDecimal collectedOn, long tasksCount) {}
    public record ArrivalDetail(Long bookingId, String client, String room, String roomType, int nights, boolean advancePaid, BigDecimal balance, String status) {}
    public record DepartureDetail(Long bookingId, String client, String room, BigDecimal balance, boolean hasItemsToInvoice, String status) {}
    public record Event(LocalDate date, String type, String label, String client, Integer guests, boolean advancePaid, Boolean withCaterer, String depositStatus, Long bookingId) {}
    public record TaskItem(Long bookingId, String reference, String client, String detail) {}
    public record TaskGroup(long count, List<TaskItem> items) {}
    public record Tasks(TaskGroup pendingConfirmation, TaskGroup lateArrivals, TaskGroup overdueDepartures, TaskGroup refundsDue, TaskGroup readyToClose, TaskGroup toInvoice, TaskGroup depositsToReturn, TaskGroup unpaidInvoices) {}
    public record CashLine(String paymentMethod, BigDecimal collected, BigDecimal refunded, BigDecimal depositsIn, BigDecimal depositsOut, String employee) {}
    public record Cash(List<CashLine> lines, BigDecimal total) {}
    public record RoomLive(Long bookingId, String room, String roomType, String state, String client, LocalDate checkIn, LocalDate checkOut, BigDecimal balance) {}
    public record SpaceLive(String space, String state, List<LocalDate> nextDates) {}
    public record FreeRoom(Long roomId, String room, String roomType, BigDecimal nightlyRate) {}
    public record RatePoint(LocalDate date, BigDecimal value) {}
    public record Month(List<Activity> revenueByActivity, List<RatePoint> occupancySeries) {}
}
