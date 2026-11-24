package mg.lagrace.api.reference.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.auth.User;
import mg.lagrace.api.auth.persistence.UserRepository;
import mg.lagrace.api.media.PhotoItemType;

@Component
@RequiredArgsConstructor
class UserResolver implements ItemResolver {
    private final UserRepository repo;

    @Override
    public PhotoItemType supports() {
        return PhotoItemType.USER;
    }

    @Override
    public boolean exists(Long id) {
        return repo.existsById(id);
    }

    @Override
    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getIdUser,
                        user -> user.getFirstName() + " " + user.getLastName()));
    }
}
