package mg.lagrace.api.reference.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.media.PhotoItemType;
import mg.lagrace.api.employee.persistence.EmployeeRepository;

@Component
@RequiredArgsConstructor
class EmployeeResolver implements ItemResolver {
    private final EmployeeRepository repo;

    public PhotoItemType supports() { return PhotoItemType.EMPLOYEE; }

    public boolean exists(Long id) { return repo.existsById(id); }

    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getIdEmployee,
                        e -> e.getFirstName() + " " + e.getLastName()));
    }
}
