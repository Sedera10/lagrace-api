package mg.lagrace.api.auth.dto.user;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountProfileRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        LocalDate birthDate,
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Size(max = 255) String address) {}
