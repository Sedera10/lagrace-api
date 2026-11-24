package mg.lagrace.api.media;

import mg.lagrace.api.catalog.pool.Pool;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.employee.Employee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PhotoItemType {
    ROOM("rooms"),
    ROOM_TYPE("room-types"),
    SPACE("spaces"),
    POOL("pools"),
    EMPLOYEE("employees"),
    USER("users");

    private final String folder;   // dossier Cloudinary : hotel/<folder>/<itemId>
}