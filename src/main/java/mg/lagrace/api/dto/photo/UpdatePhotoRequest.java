package mg.lagrace.api.dto.photo;

public record UpdatePhotoRequest(
        String caption,
        String altText,
        Integer position,   // null = inchangé
        Boolean cover       // null = inchangé
) {}
