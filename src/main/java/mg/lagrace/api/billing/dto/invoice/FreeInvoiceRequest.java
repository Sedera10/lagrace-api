package mg.lagrace.api.billing.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FreeInvoiceRequest(
        @NotNull @Valid Customer customer,
        @Size(max = 100) String referenceLabel,
        LocalDate periodStart,
        LocalDate periodEnd,
        @NotEmpty List<@Valid Line> lines,
        @DecimalMin("0.00") BigDecimal discount,
        @Valid Payment payment,
        @Size(max = 2000) String notes,
        @NotNull @DecimalMin("0.00") BigDecimal expectedTotal) {

    public record Customer(
            Long id,
            @Size(max = 200) String name,
            @Size(max = 30) String phone,
            @Size(max = 100) String nationality,
            @Size(max = 255) String address,
            @Size(max = 100) String firstname,
            @Size(max = 100) String lastname,
            @Size(max = 255) String email,
            LocalDate dateOfBirth,
            Long nationalityId,
            Long documentId,
            @Size(max = 100) String identificationNumber) {}

    public record Line(
            @NotBlank String serviceCode,
            @NotBlank @Size(max = 150) String description,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @DecimalMin("0.01") BigDecimal unitPrice) {}

    public record Payment(
            @NotBlank String status,
            Long methodId,
            @DecimalMin("0.01") BigDecimal amount) {}
}