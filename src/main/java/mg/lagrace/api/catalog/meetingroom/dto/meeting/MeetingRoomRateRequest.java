package mg.lagrace.api.catalog.meetingroom.dto.meeting;

import mg.lagrace.api.catalog.meetingroom.MeetingRoom;

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
