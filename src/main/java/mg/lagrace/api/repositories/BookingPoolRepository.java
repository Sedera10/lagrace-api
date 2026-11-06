package mg.lagrace.api.repositories;

import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.lagrace.api.models.BookingPool;

public interface BookingPoolRepository extends JpaRepository<BookingPool, Long> {
    boolean existsByRef(String ref);
    List<BookingPool> findAllByBooking_IdBooking(Long bookingId);

    @Query("""
        SELECT COUNT(line) > 0 FROM BookingPool line
        WHERE line.pool.idPool = :poolId
          AND line.active = true
          AND line.booking.idBooking <> :bookingId
          AND (
            (:exclusive = true AND (
                (line.poolService.isExculusive = true AND line.startsAt < :endsAt AND line.endsAt > :startsAt)
                OR (line.poolService.isExculusive = false AND line.startsAt >= :startsAt AND line.startsAt < :endsAt)
            ))
            OR (:exclusive = false AND line.poolService.isExculusive = true
                AND line.startsAt <= :startsAt AND line.endsAt > :startsAt)
          )
        """)
    boolean existsConflictingBooking(@Param("poolId") Long poolId,
                                     @Param("bookingId") Long bookingId,
                                     @Param("exclusive") boolean exclusive,
                                     @Param("startsAt") OffsetDateTime startsAt,
                                     @Param("endsAt") OffsetDateTime endsAt);
}
