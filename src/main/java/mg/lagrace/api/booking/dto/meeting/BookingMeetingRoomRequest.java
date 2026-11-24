package mg.lagrace.api.booking.dto.meeting;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.room.Room;

import mg.lagrace.api.booking.dto.room.BookingRoomRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import mg.lagrace.api.booking.core.BookingOrigin;
import mg.lagrace.api.booking.dto.room.BookingRoomRequest.ExtraItem;
import mg.lagrace.api.booking.dto.room.BookingRoomRequest.NewCustomer;

public record BookingMeetingRoomRequest(
        Long customerId,
        @Valid NewCustomer newCustomer,
        BookingOrigin origin,
        String notes,
        @NotNull @NotEmpty @Valid List<@NotNull MeetingRoomItem> meetingRooms,
        List<@Valid ExtraItem> extras,
        Boolean confirm,
        @DecimalMin(value = "0.00") BigDecimal expectedTotal) {

    public record MeetingRoomItem(
            @NotNull Long meetingRoomId,
            @NotNull Long serviceId,
            @NotNull LocalDate serviceDate,
            @Min(1) Integer guestsCount) {}

}