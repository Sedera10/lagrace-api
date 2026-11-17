package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.models.BookingRoom;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    boolean existsByRef(String ref);
    List<BookingRoom> findAllByBooking_IdBooking(Long bookingId);

    @Query("""
        SELECT CASE WHEN COUNT(br) = 0 THEN TRUE ELSE FALSE END
        FROM BookingRoom br
        WHERE br.room.idRoom = :roomId
            AND br.active = true 
            AND br.booking.status.code NOT IN ('CLOSED', 'CANCELED')
            AND (:excludeId IS NULL OR br.idBookingRoom <> :excludeId)
        AND br.checkIn < :checkOut
        AND br.checkOut > :checkIn
    """)
    Boolean isDispo(
        @Param("roomId") Long roomId,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT br FROM BookingRoom br
        WHERE br.room.idRoom = :roomId
            AND br.active = true
            AND br.booking.status.code NOT IN ('CLOSED', 'CANCELED')
            AND (:excludeId IS NULL OR br.idBookingRoom <> :excludeId)
            AND br.checkIn < :toDate
            AND br.checkOut > :fromDate
        ORDER BY br.checkIn
    """)
    List<BookingRoom> findConflicts(
        @Param("roomId") Long roomId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT CASE WHEN COUNT(br) > 0 THEN TRUE ELSE FALSE END
        FROM BookingRoom br
        WHERE br.room.idRoom = :roomId
          AND br.active = true
          AND br.idBookingRoom <> :excludeId
          AND br.status.code = 'CHECK_IN'
    """)
    Boolean existsOtherCheckIn(@Param("roomId") Long roomId, @Param("excludeId") Long excludeId);
}
