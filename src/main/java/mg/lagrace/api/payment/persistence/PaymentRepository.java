package mg.lagrace.api.payment.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.payment.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByBooking_IdBooking(Long bookingId);
    Optional<Payment> findByBooking_IdBookingAndRequestId(Long bookingId, UUID requestId);

}