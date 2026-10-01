package mg.lagrace.api.dto.meeting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeetingRoomRateRequest(
        Long meetingRoomId,
        Long serviceId,
        Long unitId,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo
) {}
