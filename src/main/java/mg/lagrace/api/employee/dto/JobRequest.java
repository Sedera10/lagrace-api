package mg.lagrace.api.employee.dto;

import mg.lagrace.api.employee.Employee;

public record JobRequest(String name, Long departmentId) {
}
