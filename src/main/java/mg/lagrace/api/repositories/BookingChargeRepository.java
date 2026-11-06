package mg.lagrace.api.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.BookingCharge;

public interface BookingChargeRepository extends JpaRepository<BookingCharge, Long> {
    boolean existsByRef(String ref);
    List<BookingCharge> findAllByBooking_IdBooking(Long bookingId);

}
