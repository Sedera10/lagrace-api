package mg.lagrace.api.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRef(String ref);
    Optional<Booking> findByRef(String ref);
    List<Booking> findAllByCustomer_IdCustomerOrderByCreatedAtDesc(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "status"})
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "status"})
    Page<Booking> findAllByCustomer_IdCustomerOrderByCreatedAtDesc(Long customerId, Pageable pageable);

}
