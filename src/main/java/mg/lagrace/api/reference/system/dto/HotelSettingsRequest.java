package mg.lagrace.api.reference.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelSettingsRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Size(max = 255) String address,
        @Size(max = 50) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String nif,
        @Size(max = 50) String stat,
        @Size(max = 4000) String paymentDetails,
        @Size(max = 4000) String invoiceFooter) {}
