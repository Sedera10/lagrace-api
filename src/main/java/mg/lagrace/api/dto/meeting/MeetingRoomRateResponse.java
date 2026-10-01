package mg.lagrace.api.dto.meeting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MeetingRoomRateResponse(
        Long idRate,
        Long meetingRoomId,
        Long serviceId,
        String serviceCode,
        String serviceName,
        Long unitId,
        String unitName,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        OffsetDateTime createdAt
) {}
