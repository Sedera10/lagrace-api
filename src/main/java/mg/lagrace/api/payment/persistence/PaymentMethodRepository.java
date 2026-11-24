package mg.lagrace.api.payment.persistence;

import mg.lagrace.api.payment.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.payment.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    boolean existsByName(String name);
}
