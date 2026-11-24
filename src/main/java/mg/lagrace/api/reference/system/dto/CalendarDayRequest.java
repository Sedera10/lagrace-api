package mg.lagrace.api.reference.system.dto;

import java.time.LocalDate;

public record CalendarDayRequest(LocalDate day, Boolean holiday) {}
