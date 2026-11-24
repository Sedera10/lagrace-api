package mg.lagrace.api.booking.charge.persistence;

import mg.lagrace.api.booking.core.Booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.booking.charge.BookingOther;

public interface BookingOtherRepository extends JpaRepository<BookingOther, Long> {
    List<BookingOther> findAllByBooking_IdBookingOrderByCreatedAtDesc(Long bookingId);
}
