package mg.lagrace.api.dto.room.photo;

import mg.lagrace.api.models.RoomPhoto;

public record RoomPhotoResponse(
        Long id,
        String url,
        String caption,
        boolean isCover,
        Long roomId,
        Long typeId
) {
    public static RoomPhotoResponse from(RoomPhoto photo) {
        return new RoomPhotoResponse(
                photo.getIdPhoto(),
                photo.getUrl(),
                photo.getCaption(),
                photo.getIsCover(),
                photo.getRoom() != null ? photo.getRoom().getIdRoom() : null,
                photo.getType() != null ? photo.getType().getIdType() : null
        );
    }
}
