package mg.lagrace.api.auth.dto.user;

import mg.lagrace.api.auth.User;

import java.time.LocalDate;
import java.util.Set;

public record UserResponse(
        Long idUser,
        String lastName,
        String firstName,
        LocalDate birthDate,
        String username,
        String email,
        boolean active,
        Set<String> roles,
        Set<Long> roleIds) {
}