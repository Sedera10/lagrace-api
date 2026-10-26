package mg.lagrace.api.dto.room.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// Le type et l'unité ne changent jamais : on corrige le montant et la période
public record UpdateRoomRateRequest(
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {}
