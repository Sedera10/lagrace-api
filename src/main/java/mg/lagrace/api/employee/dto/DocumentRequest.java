package mg.lagrace.api.employee.dto;

import mg.lagrace.api.employee.Employee;

public record DocumentRequest(
        Long employeeId,
        Long typeId,
        String documentNumber,
        String filePath) {
}
