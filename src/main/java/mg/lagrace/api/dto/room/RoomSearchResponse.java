package mg.lagrace.api.dto.room;

import java.math.BigDecimal;
import java.time.LocalDate;

import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.repositories.RoomRateRepository.ApplicableNightRate;

public record RoomSearchResponse(
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
        String rateUnit,
        LocalDate rateDate) {

    public static RoomSearchResponse from(RoomDetail room, ApplicableNightRate rate, LocalDate rateDate) {
        return new RoomSearchResponse(
                room.getIdRoom(),
                room.getRefRoom(),
                room.getFloor(),
                room.getZone(),
                room.getDescription(),
                room.getStatusId(),
                room.getStatus(),
                room.getColor(),
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
                rate != null ? rate.getUnitName() : null,
                rateDate);
    }
}