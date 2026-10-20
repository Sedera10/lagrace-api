package mg.lagrace.api.models.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.models.Pool;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.repositories.PoolRepository;

@Component
@RequiredArgsConstructor
class PoolResolver implements ItemResolver {
    private final PoolRepository repo;

    @Override
    public PhotoItemType supports() {
        return PhotoItemType.POOL;
    }

    @Override
    public boolean exists(Long id) {
        return repo.existsById(id);
    }

    @Override
    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(Pool::getIdPool, Pool::getName));
    }
}
