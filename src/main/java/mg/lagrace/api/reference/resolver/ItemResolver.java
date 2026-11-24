package mg.lagrace.api.reference.resolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mg.lagrace.api.media.PhotoItemType;

public interface ItemResolver {
    PhotoItemType supports();
    boolean exists(Long itemId);
    Map<Long, String> getNames(Collection<Long> itemIds);

    default String getName(Long itemId) {
        return getNames(List.of(itemId)).getOrDefault(itemId, "Inconnu");
    }
}
