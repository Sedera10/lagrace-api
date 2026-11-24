package mg.lagrace.api.catalog.room.dto.room;

import mg.lagrace.api.catalog.room.Room;

import java.time.OffsetDateTime;

import mg.lagrace.api.catalog.room.RoomStatusHistory;

public record RoomStatusHistoryResponse(
        Long idHistory,
        Long statusId,
        String statusName,
        String colorCode,
        OffsetDateTime changedAt
) {
    public static RoomStatusHistoryResponse from(RoomStatusHistory history) {
        return new RoomStatusHistoryResponse(
                history.getIdHistory(),
                history.getStatus().getIdStatus(),
                history.getStatus().getName(),
                history.getStatus().getColorCode(),
                history.getChangedAt()
        );
    }
}
