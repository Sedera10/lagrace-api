package mg.lagrace.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import mg.lagrace.api.models.User;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> getUsers(){
        return userRepo.findAll();
    }
}
