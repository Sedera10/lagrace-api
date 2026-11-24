package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerBookingsResponse(List<Item> items, Totals totals, int page, int size) {
    public record Item(Long bookingId, String bookingRef, LocalDate periodStart, LocalDate periodEnd,
            List<String> services, String statusCode, String statusName, String origin,
            BigDecimal total, BigDecimal paid, BigDecimal balance) {}
    public record Totals(long count, BigDecimal total, BigDecimal paid, BigDecimal balance) {}
}
