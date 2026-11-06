package mg.lagrace.api.dto.booking.room;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import mg.lagrace.api.models.enums.BookingOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record BookingRoomRequest(

        // --- Client : exactement un des deux ---
        Long customerId,
        @Valid NewCustomer newCustomer,

        Long userId,

        BookingOrigin origin,                       // facultatif
        @Size(max = 2000) String notes,

        @NotEmpty(message = "Au moins une chambre est requise")
        List<@Valid @NotNull RoomItem> rooms,

        List<@Valid @NotNull ExtraItem> extras,     // facultatif (null ou vide accepté)

        @Valid AdvanceItem advance,                 // facultatif
        Boolean confirm,

        @PositiveOrZero BigDecimal expectedTotal    // facultatif : total affiché à l'écran
) {

    // ----------------------------------------------------------------
    public record NewCustomer(
            @NotBlank @Size(max = 100) String lastname,
            @NotBlank @Size(max = 100) String firstname,
            LocalDate dateOfBirth,
            @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Size(max = 255) String address,
            Long nationalityId,                  // facultatif
            Long documentId,                     // facultatif, ensemble avec le numéro
            @Size(max = 100) String identificationNumber
    ) {
        @JsonIgnore
        @AssertTrue(message = "Le type de document et le numéro vont ensemble")
        public boolean isDocumentConsistent() {
            boolean hasDoc = documentId != null;
            boolean hasNumber = identificationNumber != null && !identificationNumber.isBlank();
            return hasDoc == hasNumber;
        }
    }

    // ----------------------------------------------------------------
    public record RoomItem(
            @NotNull Long roomId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut
    ) {
        @JsonIgnore
        @AssertTrue(message = "La date de départ doit être après la date d'arrivée")
        public boolean isPeriodValid() {
            return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
        }
    }

    // ----------------------------------------------------------------
    public record ExtraItem(
            Long menuItemId,                     // restaurant
            Integer additionalServiceId,            // service additionnel
            @NotNull @Min(1) Integer quantity
    ) {
        @JsonIgnore
        @AssertTrue(message = "Indiquez soit menuItemId, soit additionalServiceId (un seul)")
        public boolean isSingleSource() {
            return (menuItemId != null) ^ (additionalServiceId != null);
        }
    }

    // ----------------------------------------------------------------
    public record AdvanceItem(
            @NotNull Long methodId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount
    ) {}

    // ----------------------------------------------------------------
    // Règle sur le client : exactement un des deux
    @JsonIgnore
    @AssertTrue(message = "Indiquez soit customerId, soit newCustomer (un seul)")
    public boolean isCustomerChosen() {
        return (customerId != null) ^ (newCustomer != null);
    }
}