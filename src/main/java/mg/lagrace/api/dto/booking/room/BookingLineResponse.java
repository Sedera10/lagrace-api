package mg.lagrace.api.dto.booking.room;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import mg.lagrace.api.models.views.BookingLineView;

public record BookingLineResponse(
        String lineType,
        Long lineId,
        String reference,
        Long resourceId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer quantity,
        BigDecimal unitPrice,
        Boolean included,
        BigDecimal amount,
        Long statusId,
        String statusCode,
        Boolean active,
        Boolean main) {

    public static BookingLineResponse from(BookingLineView line) {
        return new BookingLineResponse(
                line.getId().getLineType(),
                line.getId().getLineId(),
                line.getReference(),
                line.getResourceId(),
                line.getStartsAt(),
                line.getEndsAt(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getIncluded(),
                line.getAmount(),
                line.getStatusId(),
                line.getStatusCode(),
                line.getActive(),
                false);
    }
}