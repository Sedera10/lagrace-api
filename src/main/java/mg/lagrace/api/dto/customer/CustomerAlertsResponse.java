package mg.lagrace.api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CustomerAlertsResponse(String level, List<String> levelReasons,
        List<CustomerFileResponse.Tag> tags, BigDecimal balanceDue,
        List<BalanceDueBooking> balanceDueBookings, BigDecimal refundDue,
        List<AlertNote> alertNotes, LocalDate nextVisit, boolean hasOpenBooking) {
    public record BalanceDueBooking(Long bookingId, String bookingRef, BigDecimal balance) {}
    public record AlertNote(Long id, String noteType, String content, OffsetDateTime createdAt) {}
}