package mg.lagrace.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import mg.lagrace.api.dto.UserResponse;
import mg.lagrace.api.models.Role;
import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<UserResponse> getUsers() {
        return userRepo.findAll().stream()
                .map(user -> new UserResponse(
                        user.getIdUser(),
                        user.getLastName(),
                        user.getFirstName(),
                        user.getBirthDate(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isActive(),
                        user.getRoles()
                                .stream()
                                .map(Role::getName)
                                .collect(Collectors.toSet())
                ))
                .toList();
    }
}
