package mg.lagrace.api.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhotoItemType {
    ROOM("rooms"),
    ROOM_TYPE("room-types"),
    SPACE("spaces"),
    POOL("pools"),
    EMPLOYEE("employees");

    private final String folder;   // dossier Cloudinary : hotel/<folder>/<itemId>
}