package mg.lagrace.api.catalog.space.dto.banquet;

import mg.lagrace.api.catalog.space.Space;

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