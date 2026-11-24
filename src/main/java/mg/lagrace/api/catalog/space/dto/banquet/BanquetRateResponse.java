package mg.lagrace.api.catalog.space.dto.banquet;

import mg.lagrace.api.catalog.space.Space;

import java.math.BigDecimal;
import java.time.LocalDate;

import mg.lagrace.api.catalog.space.BanquetRate;

public record BanquetRateResponse(
        Long idRate,
        Long menuTypeId,
        String menuTypeCode,
        String menuTypeName,
        Integer minPersons,
        Integer maxPersons,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo) {

    public static BanquetRateResponse from(BanquetRate rate) {
        return new BanquetRateResponse(
                rate.getIdRate(),
                rate.getMenuType().getIdMenuType(),
                rate.getMenuType().getCode(),
                rate.getMenuType().getName(),
                rate.getMinPersons(),
                rate.getMaxPersons(),
                rate.getAmount(),
                rate.getValidFrom(),
                rate.getValidTo());
    }
}