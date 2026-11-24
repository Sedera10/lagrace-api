package mg.lagrace.api.booking.dto;

import jakarta.validation.constraints.Min;

public record BookingChargeCreateRequest(Long menuItemId, Long additionalServiceId,
    @Min(1) Integer quantity, Boolean included) {
    public boolean hasSingleCatalogSource() { return (menuItemId != null) ^ (additionalServiceId != null); }
}
