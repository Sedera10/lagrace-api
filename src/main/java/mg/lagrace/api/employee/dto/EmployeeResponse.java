package mg.lagrace.api.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
        Long id, String lastName, String firstName, LocalDate dateOfBirth,
        String phone, String address, Long sexId, String sexName,
        Long nationalityId, String nationalityName, Long jobId, String jobName,
        Long departmentId, String departmentName, Long contractTypeId,
        String contractTypeName, LocalDate hireDate, LocalDate endDate,
        BigDecimal baseSalary, Long userId) { }
