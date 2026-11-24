package mg.lagrace.api.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record BookingSpaceUpdateRequest(
        @NotNull Integer version,
                Long spaceId,
                LocalDate eventDate,
                Integer guestsCount,
                Long eventTypeId,
                Boolean withCaterer,
                String reason,
                BigDecimal expectedNewTotal,
                Boolean confirmImpact,
                List<ShiftLinkedRoom> shiftLinked) {

        public record ShiftLinkedRoom(Long bookingRoomId, LocalDate newCheckIn, LocalDate newCheckOut) {}

        public BookingSpaceUpdateRequest(Integer version, LocalDate eventDate, Integer guestsCount,
                        Boolean withCaterer, BigDecimal expectedNewTotal) {
                this(version, null, eventDate, guestsCount, null, withCaterer, null, expectedNewTotal, null, null);
        }
}