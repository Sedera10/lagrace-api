package mg.lagrace.api.dashboard.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.dashboard.BookingBalance;

public interface BookingBalanceRepository extends JpaRepository<BookingBalance, Long> {
}