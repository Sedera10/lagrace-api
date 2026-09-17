package mg.lagrace.api.dto.system;

import java.time.LocalDate;

public record CalendarDayRequest(LocalDate day, Boolean holiday) {}
