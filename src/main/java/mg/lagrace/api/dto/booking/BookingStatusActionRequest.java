package mg.lagrace.api.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingStatusActionRequest(@NotNull Integer version, @Size(max = 255) String reason) {
	public BookingStatusActionRequest(Integer version) {
		this(version, null);
	}
}