package mg.lagrace.api.leave.dto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
public final class LeaveDtos { private LeaveDtos() {}
 public record Create(Long employeeId, Long leaveTypeId, LocalDate startDate, LocalDate endDate, String notes) {}
 public record Update(Long leaveTypeId, LocalDate startDate, LocalDate endDate, String notes) {}
 public record TypeRequest(String name) {}
 public record TypeResponse(Long id, String name) {}
 public record Item(Long id, Long employeeId, String employeeName, Long typeId, String typeName, LocalDate startDate, LocalDate endDate, long days, String state, String notes, String approvedByName, OffsetDateTime createdAt) {}
 public record Totals(long count, long totalDays, long ongoingCount, long upcomingCount) {}
 public record Page(List<Item> items, Totals totals, int page, int size) {}
 public record Summary(Long id, String typeName, LocalDate startDate, LocalDate endDate) {}
 public record Overview(Long employeeId, String employeeName, Summary current, Summary last, Summary next) {}
}
