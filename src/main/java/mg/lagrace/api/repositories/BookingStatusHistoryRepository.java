package mg.lagrace.api.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.BookingStatusHistory;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findAllByBooking_IdBookingOrderByChangedAtDesc(Long bookingId);
    List<BookingStatusHistory> findTop30ByBooking_IdBookingOrderByChangedAtDesc(Long bookingId);
}
