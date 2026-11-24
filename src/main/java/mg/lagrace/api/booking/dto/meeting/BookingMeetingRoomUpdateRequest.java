package mg.lagrace.api.booking.dto.meeting;

import mg.lagrace.api.booking.core.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nullable resource fields mean "leave unchanged". */
public record BookingMeetingRoomUpdateRequest(Integer version, Long meetingRoomId, Long serviceId,
        LocalDate serviceDate, Integer guestsCount, String reason, BigDecimal expectedNewTotal) {}
