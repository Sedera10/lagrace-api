package mg.lagrace.api.booking.core;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingLineStatusRepository extends JpaRepository<BookingLineStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    BookingLineStatus findByCode(String code);
}
