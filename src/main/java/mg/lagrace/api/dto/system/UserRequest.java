package mg.lagrace.api.dto.system;

import java.time.LocalDate;
import java.util.Set;

public record UserRequest(
        String lastName,
        String firstName,
        LocalDate birthDate,
        String username,
        String email,
        String password,
        Boolean active,
        Set<Long> roleIds) {}
