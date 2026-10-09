package mg.lagrace.api.models.resolver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.models.Room;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.repositories.RoomRepository;

@Component
@RequiredArgsConstructor
class RoomResolver implements ItemResolver {
    private final RoomRepository repo;

    public PhotoItemType supports() { return PhotoItemType.ROOM; }

    public boolean exists(Long id) { return repo.existsById(id); }

    public Map<Long, String> getNames(Collection<Long> ids) {
        return repo.findAllById(ids).stream()
                .collect(Collectors.toMap(Room::getIdRoom, r -> "Chambre " + r.getRefRoom()));
    }
}
