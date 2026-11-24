package mg.lagrace.api.employee.dto;

import java.time.OffsetDateTime;

public record DocumentResponse(
        Long id, Long employeeId, String employeeName, Long typeId, String typeName,
        String documentNumber, String filePath, OffsetDateTime createdAt) { }
