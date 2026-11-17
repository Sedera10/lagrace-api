package mg.lagrace.api.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceLinkResponse(Long id, String number, LocalDate issueDate, BigDecimal total,
        String type, String status) {}