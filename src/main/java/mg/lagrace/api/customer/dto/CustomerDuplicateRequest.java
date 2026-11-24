package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CustomerDuplicateRequest(@NotBlank @Size(max = 100) String lastname,
        @NotBlank @Size(max = 100) String firstname, @Size(max = 30) String phone,
        LocalDate dateOfBirth, Long documentId, @Size(max = 100) String identificationNumber) {}