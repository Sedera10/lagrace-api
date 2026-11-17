package mg.lagrace.api.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import mg.lagrace.api.models.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRef(String ref);
    Optional<Booking> findByRef(String ref);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.idBooking = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
    List<Booking> findAllByCustomer_IdCustomerOrderByCreatedAtDesc(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"customer", "status"})
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "status"})
    Page<Booking> findAllByCustomer_IdCustomerOrderByCreatedAtDesc(Long customerId, Pageable pageable);

}
