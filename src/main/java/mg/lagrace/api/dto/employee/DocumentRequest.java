package mg.lagrace.api.dto.employee;

public record DocumentRequest(
        Long employeeId,
        Long typeId,
        String documentNumber,
        String filePath) {
}
