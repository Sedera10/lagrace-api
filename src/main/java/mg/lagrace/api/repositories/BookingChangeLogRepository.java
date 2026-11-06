package mg.lagrace.api.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.BookingChangeLog;

public interface BookingChangeLogRepository extends JpaRepository<BookingChangeLog, Long> {
    List<BookingChangeLog> findTop30ByBookingIdOrderByChangedAtDesc(Long bookingId);
}
