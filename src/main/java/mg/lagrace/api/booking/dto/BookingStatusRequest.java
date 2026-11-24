package mg.lagrace.api.booking.dto;

import jakarta.validation.constraints.Pattern;

public record BookingStatusRequest(
        String code,
        String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit être au format #RRGGBB.")
        String colorCode) {}