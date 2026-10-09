package mg.lagrace.api.models.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.repositories.SpaceRepository;

@Component
@RequiredArgsConstructor
class SpaceResolver implements ItemResolver {
    private final SpaceRepository repo;

    public PhotoItemType supports() { return PhotoItemType.SPACE; }

    public boolean exists(Long id) { return repo.existsById(id); }

    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(Space::getIdSpace, Space::getName));
    }
}
