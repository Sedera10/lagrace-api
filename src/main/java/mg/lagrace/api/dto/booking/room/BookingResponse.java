package mg.lagrace.api.dto.booking.room;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import mg.lagrace.api.models.enums.BookingOrigin;

public record BookingResponse(
        Long bookingId,
        String reference,
        Long customerId,
        String customerFirstName,
        String customerLastName,
        String customerPhone,
        String statusCode,
        String statusName,
        String statusColor,
        BookingOrigin origin,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        BigDecimal linesTotal,
        BigDecimal chargesTotal,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        BigDecimal depositDue,
        BigDecimal depositHeld,
        List<BookingLineResponse> lines) {
}