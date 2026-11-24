package mg.lagrace.api.media.dto.photo;

import mg.lagrace.api.media.Photo;

import mg.lagrace.api.media.PhotoItemType;

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
