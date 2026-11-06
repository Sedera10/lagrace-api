package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record BookingChargeCreateRequest(Long menuItemId, Long additionalServiceId,
        @Min(1) Integer quantity) {
    public boolean hasSingleCatalogSource() { return (menuItemId != null) ^ (additionalServiceId != null); }
}
