package mg.lagrace.api.booking.core;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findAllByBooking_IdBookingOrderByChangedAtDesc(Long bookingId);
    List<BookingStatusHistory> findTop30ByBooking_IdBookingOrderByChangedAtDesc(Long bookingId);
}
