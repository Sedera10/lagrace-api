package mg.lagrace.api.booking.core;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusRepository extends JpaRepository<BookingStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    BookingStatus findByCode(String code);
}
