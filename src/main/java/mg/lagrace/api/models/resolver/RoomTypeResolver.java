package mg.lagrace.api.models.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.repositories.RoomTypeRepository;

@Component
@RequiredArgsConstructor
class RoomTypeResolver implements ItemResolver {
    private final RoomTypeRepository repo;

    public PhotoItemType supports() { return PhotoItemType.ROOM_TYPE; }

    public boolean exists(Long id) { return repo.existsById(id); }

    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(RoomType::getIdType, RoomType::getName));
    }
}
