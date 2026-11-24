package mg.lagrace.api.reference.system.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.reference.CalendarDay;

public interface CalendarDayRepository extends JpaRepository<CalendarDay, LocalDate> {
    List<CalendarDay> findByDayBetween(LocalDate start, LocalDate end);
}
