package mg.lagrace.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.views.BookingBalance;

public interface BookingBalanceRepository extends JpaRepository<BookingBalance, Long> {
}