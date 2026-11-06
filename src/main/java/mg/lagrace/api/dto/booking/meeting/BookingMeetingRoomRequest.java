package mg.lagrace.api.dto.booking.meeting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.NewCustomer;
import mg.lagrace.api.models.enums.BookingOrigin;

public record BookingMeetingRoomRequest(
        Long customerId,
        @Valid NewCustomer newCustomer,
        BookingOrigin origin,
        String notes,
        @NotNull @Valid List<@NotNull MeetingRoomItem> meetingRooms,
        @Valid PaymentItem advance,
        List<@Valid ExtraItem> extras,
        Boolean confirm,
        @DecimalMin(value = "0.00") BigDecimal expectedTotal) {

    public record MeetingRoomItem(
            @NotNull Long meetingRoomId,
            @NotNull Long serviceId,
            @NotNull LocalDate serviceDate,
            @Min(1) Integer guestsCount) {}

    public record PaymentItem(@NotNull Long methodId, @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}
}