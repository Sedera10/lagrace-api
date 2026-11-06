package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.BookingOther;

public interface BookingOtherRepository extends JpaRepository<BookingOther, Long> {
    List<BookingOther> findAllByBooking_IdBookingOrderByCreatedAtDesc(Long bookingId);
}
