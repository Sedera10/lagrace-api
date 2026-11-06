package mg.lagrace.api.dto.customer;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import mg.lagrace.api.models.Customer;

public record CustomerResponse(
        Long idCustomer,
        String lastName,
        String firstName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String address,
        String identificationNumber,
        Long documentId,
        String documentName,
        Long nationalityId,
        String nationalityName,
        String nationalityCountryCode,
        String nationalityFlagEmoji,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getIdCustomer(),
                customer.getLastName(),
                customer.getFirstName(),
                customer.getDateOfBirth(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getIdentificationNumber(),
                customer.getDocument() != null ? customer.getDocument().getIdDocument() : null,
                customer.getDocument() != null ? customer.getDocument().getName() : null,
                customer.getNationality() != null ? customer.getNationality().getIdNationality() : null,
                customer.getNationality() != null ? customer.getNationality().getName() : null,
                customer.getNationality() != null ? customer.getNationality().getCountryCode() : null,
                customer.getNationality() != null ? customer.getNationality().getFlagEmoji() : null,
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}