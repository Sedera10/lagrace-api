package mg.lagrace.api.booking.dto;

import jakarta.validation.constraints.Size;

public record BookingChargeVoidRequest(Integer version, @Size(max = 255) String reason) {}
