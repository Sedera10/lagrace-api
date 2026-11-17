package mg.lagrace.api.dto.room;

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
