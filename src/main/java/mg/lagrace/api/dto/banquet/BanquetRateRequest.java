package mg.lagrace.api.dto.banquet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BanquetRateRequest(
        Long menuTypeId,
        Integer minPersons,
        Integer maxPersons,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo) {
}