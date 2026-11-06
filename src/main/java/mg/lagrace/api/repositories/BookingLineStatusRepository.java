package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.BookingLineStatus;

public interface BookingLineStatusRepository extends JpaRepository<BookingLineStatus, Long> {
    boolean existsByCode(String code);
    boolean existsByName(String name);
    BookingLineStatus findByCode(String code);
}
