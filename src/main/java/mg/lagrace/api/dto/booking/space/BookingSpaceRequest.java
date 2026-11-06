package mg.lagrace.api.dto.booking.space;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mg.lagrace.api.dto.booking.room.BookingRoomRequest.NewCustomer;
import mg.lagrace.api.models.enums.BookingOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingSpaceRequest(
        Long customerId,
        @Valid NewCustomer newCustomer,
        BookingOrigin origin,
        @Size(max = 2000) String notes,
        @NotNull @Valid SpaceItem space,
        List<@Valid @NotNull BanquetItem> banquet,
        List<@Valid @NotNull ExtraItem> extras,
        List<@Valid @NotNull RoomItem> rooms,
        List<@Valid @NotNull MeetingRoomItem> meetingRooms,
        @Valid AdvanceItem advance,
        Boolean confirm,
        @PositiveOrZero BigDecimal expectedTotal) {

    @JsonIgnore
    @AssertTrue(message = "Indiquez exactement un client existant ou un nouveau client")
    public boolean isCustomerChosen() {
        return (customerId != null) ^ (newCustomer != null);
    }

    public record SpaceItem(
            @NotNull Long spaceId,
            Long eventTypeId,
            @NotNull LocalDate eventDate,
            @NotNull @Positive Integer guestsCount,
            @NotNull Boolean withCaterer) {}

    public record BanquetItem(
            @NotNull Long menuTypeId,
            @NotNull @Positive Integer persons) {}

    public record ExtraItem(
            Long additionalServiceId,
            Long menuItemId,
            @NotNull @Min(1) Integer quantity) {
        @JsonIgnore
        @AssertTrue(message = "Indiquez une seule source par extra")
        public boolean isSingleSource() {
            return (additionalServiceId != null) ^ (menuItemId != null);
        }
    }

    public record RoomItem(
            @NotNull Long roomId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            Boolean included) {
        @JsonIgnore
        @AssertTrue(message = "La date de départ doit être après la date d'arrivée")
        public boolean isPeriodValid() {
            return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
        }
    }

    public record MeetingRoomItem(
            @NotNull Long meetingRoomId,
            @NotNull Long serviceId,
            @NotNull LocalDate serviceDate,
            Boolean included) {}

    public record AdvanceItem(
            @NotNull Long methodId,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}
}