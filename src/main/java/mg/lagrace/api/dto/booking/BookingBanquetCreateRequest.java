package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookingBanquetCreateRequest(
        @NotNull Long menuTypeId,
        @NotNull @Positive Integer persons) {}