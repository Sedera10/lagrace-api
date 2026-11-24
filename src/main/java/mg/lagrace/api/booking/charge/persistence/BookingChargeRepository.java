package mg.lagrace.api.booking.charge.persistence;

import mg.lagrace.api.booking.core.Booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.booking.charge.BookingCharge;

public interface BookingChargeRepository extends JpaRepository<BookingCharge, Long> {
    boolean existsByRef(String ref);
    List<BookingCharge> findAllByBooking_IdBooking(Long bookingId);

}
