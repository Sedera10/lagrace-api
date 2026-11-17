package mg.lagrace.api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerInvoicesResponse(List<Item> items, int page, int size, long totalItems) {
    public record Item(Long id, String number, LocalDate issueDate, Long bookingId, BigDecimal total,
            BigDecimal netTotal, String paymentStatus, String status, String type) {}
}
