package mg.lagrace.api.dto.booking.room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingRoomResponse(
        Long bookingId,
        String reference,
        Long customerId,
        String status,
        List<RoomLine> rooms,
        List<ChargeLine> charges,
        BigDecimal total,
        BigDecimal balance,
        BigDecimal depositDue,
        BigDecimal depositHeld
) {
    public record RoomLine(
            Long bookingRoomId,
            String reference,
            Long roomId,
            String roomReference,
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {}

    public record ChargeLine(
            Long chargeId,
            String reference,
            String label,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            Boolean deposit
    ) {}
}