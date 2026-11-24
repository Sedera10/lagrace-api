package mg.lagrace.api.reference.resolver;

import mg.lagrace.api.catalog.room.Room;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mg.lagrace.api.catalog.room.RoomType;
import mg.lagrace.api.media.PhotoItemType;
import mg.lagrace.api.catalog.room.persistence.RoomTypeRepository;

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
