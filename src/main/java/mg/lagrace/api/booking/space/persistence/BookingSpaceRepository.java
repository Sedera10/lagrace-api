package mg.lagrace.api.booking.space.persistence;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.catalog.space.Space;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.booking.space.BookingSpace;

public interface BookingSpaceRepository extends JpaRepository<BookingSpace, Long> {
    boolean existsByRef(String ref);
    List<BookingSpace> findAllByBooking_IdBooking(Long bookingId);

    @Query("SELECT COUNT(line) > 0 FROM BookingSpace line WHERE line.space.idSpace = :spaceId AND line.eventDate = :eventDate AND line.active = true AND (:excludeId IS NULL OR line.idBookingSpace <> :excludeId)")
    boolean existsActiveBookingForSpaceAndDate(@Param("spaceId") Long spaceId,
            @Param("eventDate") java.time.LocalDate eventDate,
            @Param("excludeId") Long excludeId);

    default boolean existsActiveBookingForSpaceAndDate(Long spaceId, java.time.LocalDate eventDate) {
        return existsActiveBookingForSpaceAndDate(spaceId, eventDate, null);
    }
}
