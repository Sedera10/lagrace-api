package mg.lagrace.api.booking.core;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingChangeLogRepository extends JpaRepository<BookingChangeLog, Long> {
    List<BookingChangeLog> findTop30ByBookingIdOrderByChangedAtDesc(Long bookingId);
}
