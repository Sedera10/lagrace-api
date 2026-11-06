package mg.lagrace.api.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.PaymentKind;

public interface PaymentKindRepository extends JpaRepository<PaymentKind, Long> {
    Optional<PaymentKind> findByCode(String code);
}