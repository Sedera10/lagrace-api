package mg.lagrace.api.catalog.room.dto.room;

import java.math.BigDecimal;

import mg.lagrace.api.catalog.room.RoomDetail;
import mg.lagrace.api.catalog.room.RoomStatus;
import mg.lagrace.api.catalog.room.persistence.RoomRateRepository.ApplicableNightRate;

public record RoomDetailResponse(
        Integer idRoom,
        String refRoom,
        Integer floor,
        String zone,
        String description,
        Integer statusId,
        String status,
        String color,
        Integer typeId,
        String type,
        Integer defaultCapacity,
        Boolean airConditioned,
        Integer categoryId,
        String category,
        Integer coverPhotoId,
        String cover,
        Long rateId,
        BigDecimal rate,
        String rateUnit) {

    public static RoomDetailResponse from(
            RoomDetail room, RoomStatus status, ApplicableNightRate rate) {
        return new RoomDetailResponse(
                room.getIdRoom(),
                room.getRefRoom(),
                room.getFloor(),
                room.getZone(),
                room.getDescription(),
                status != null ? Math.toIntExact(status.getIdStatus()) : room.getStatusId(),
                status != null ? status.getName() : room.getStatus(),
                status != null ? status.getColorCode() : room.getColor(),
                room.getTypeId(),
                room.getType(),
                room.getDefaultCapacity(),
                room.getAirConditioned(),
                room.getCategoryId(),
                room.getCategory(),
                room.getCoverPhotoId(),
                room.getCover(),
                rate != null ? rate.getRateId() : null,
                rate != null ? rate.getAmount() : null,
                rate != null ? rate.getUnitName() : null);
    }
}
