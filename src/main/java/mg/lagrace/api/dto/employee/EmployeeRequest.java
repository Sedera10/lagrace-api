package mg.lagrace.api.dto.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        String lastName,
        String firstName,
        LocalDate dateOfBirth,
        String phone,
        String address,
        Long sexId,
        Long nationalityId,
        Long jobId,
        Long contractTypeId,
        LocalDate hireDate,
        LocalDate endDate,
        BigDecimal baseSalary,
        Long userId) {
}
