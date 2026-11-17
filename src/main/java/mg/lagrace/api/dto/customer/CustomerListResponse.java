package mg.lagrace.api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerListResponse(List<Item> items, Totals totals, int page, int size,
        long totalItems, int totalPages) {
    public record Item(Long id, String fullName, String phone, String nationality, List<Tag> tags,
            long bookingsCount, LocalDate lastVisit, LocalDate nextVisit, BigDecimal totalBilled,
            BigDecimal balanceDue, BigDecimal refundDue, boolean isActive) {}
    public record Tag(Long id, String name, String colorCode) {}
    public record Totals(long count, long withBalanceCount, BigDecimal balanceDueTotal) {}
}