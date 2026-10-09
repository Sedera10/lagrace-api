package mg.lagrace.api.dto.photo;

import mg.lagrace.api.models.enums.PhotoItemType;

public record PhotoResponse(
    Integer id,
    PhotoItemType itemType,
    Long itemId,
    String itemName,      // calculé, non stocké
    String url,
    String caption,
    String altText,
    int position,
    boolean cover
) {}    
