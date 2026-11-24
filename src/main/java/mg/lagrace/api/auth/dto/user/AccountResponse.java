package mg.lagrace.api.auth.dto.user;

import java.time.LocalDate;
import java.util.List;

public record AccountResponse(
        Long idUser,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String username,
        String email,
        boolean employeeLinked,
        String phone,
        String address,
        List<String> roles,
        String photoUrl) {}
