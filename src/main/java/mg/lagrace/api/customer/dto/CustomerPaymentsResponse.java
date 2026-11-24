package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CustomerPaymentsResponse(List<Item> items, Totals totals, int page, int size, long totalItems) {
    public record Item(Long id, OffsetDateTime paidAt, Long bookingId, String bookingRef, String type,
            String method, BigDecimal amount, String receivedBy, String note) {}
    public record Totals(BigDecimal received, BigDecimal refunded, BigDecimal net) {}
}
