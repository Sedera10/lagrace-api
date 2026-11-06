package mg.lagrace.api.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.lagrace.api.models.BookingSpace;

public interface BookingSpaceRepository extends JpaRepository<BookingSpace, Long> {
    boolean existsByRef(String ref);
    List<BookingSpace> findAllByBooking_IdBooking(Long bookingId);

    @Query("SELECT COUNT(line) > 0 FROM BookingSpace line WHERE line.space.idSpace = :spaceId AND line.eventDate = :eventDate AND line.active = true")
    boolean existsActiveBookingForSpaceAndDate(@Param("spaceId") Long spaceId, @Param("eventDate") java.time.LocalDate eventDate);
}
