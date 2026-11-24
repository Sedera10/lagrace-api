package mg.lagrace.api.media.dto.photo;

import mg.lagrace.api.media.Photo;

public record UpdatePhotoRequest(
        String caption,
        String altText,
        Integer position,   // null = inchangé
        Boolean cover       // null = inchangé
) {}
