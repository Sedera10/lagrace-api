package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.BookingStatus;

public interface BookingStatusRepository extends JpaRepository<BookingStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    BookingStatus findByCode(String code);
}
