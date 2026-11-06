package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.Size;

public record BookingChargeVoidRequest(Integer version, @Size(max = 255) String reason) {}
