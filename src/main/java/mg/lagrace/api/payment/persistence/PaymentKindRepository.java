package mg.lagrace.api.payment.persistence;

import mg.lagrace.api.payment.Payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.payment.PaymentKind;

public interface PaymentKindRepository extends JpaRepository<PaymentKind, Long> {
    Optional<PaymentKind> findByCode(String code);
}