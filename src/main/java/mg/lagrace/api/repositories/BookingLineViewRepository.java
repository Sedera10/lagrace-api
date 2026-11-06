package mg.lagrace.api.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.views.BookingLineView;
import mg.lagrace.api.models.views.BookingLineView.BookingLineViewId;

public interface BookingLineViewRepository extends JpaRepository<BookingLineView, BookingLineViewId> {
    @Query(value = """
        SELECT * FROM v_booking_line
        WHERE booking_id IN (:bookingIds)
        ORDER BY booking_id, starts_at NULLS LAST, line_type, line_id
        """, nativeQuery = true)
    List<BookingLineView> findAllForBookings(@Param("bookingIds") Collection<Long> bookingIds);
}