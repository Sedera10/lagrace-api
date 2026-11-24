package mg.lagrace.api.catalog.room.persistence;

import mg.lagrace.api.booking.core.Booking;
import mg.lagrace.api.booking.room.BookingRoom;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.lagrace.api.catalog.room.Room;

public interface RoomRepository extends JpaRepository<Room,Long> {
    boolean existsByRefRoom(String refRoom);

    default List<Long> findAvailableRoomIds(LocalDate checkIn, LocalDate checkOut) {
        return findAvailableRoomIds(checkIn, checkOut, null);
    }

        // L'exclusion sert à réévaluer une ligne existante : sa propre chambre reste
        // sélectionnable même si son statut opérationnel est devenu « OCCUPÉE ».
        @Query("""
                SELECT room.idRoom FROM Room room
                WHERE room.isDeleted = false
                    AND (
                        UPPER(TRIM(room.status.name)) = 'DISPONIBLE'
                        OR EXISTS (
                            SELECT currentLine.idBookingRoom FROM BookingRoom currentLine
                            WHERE currentLine.idBookingRoom = :excludeBookingRoomId
                              AND currentLine.room.idRoom = room.idRoom
                        )
                    )
                    AND NOT EXISTS (
                            SELECT bookingRoom.idBookingRoom FROM BookingRoom bookingRoom
                            WHERE bookingRoom.room.idRoom = room.idRoom
                                AND bookingRoom.active = true
                                AND bookingRoom.booking.status.code NOT IN ('CLOSED', 'CANCELED')
                                AND (:excludeBookingRoomId IS NULL OR bookingRoom.idBookingRoom <> :excludeBookingRoomId)
                                AND bookingRoom.checkIn < :checkOut
                                AND bookingRoom.checkOut > :checkIn
                    )
                ORDER BY room.idRoom
                """)
        List<Long> findAvailableRoomIds(@Param("checkIn") LocalDate checkIn,
                                                                        @Param("checkOut") LocalDate checkOut,
                                                                        @Param("excludeBookingRoomId") Long excludeBookingRoomId);

        @Query("""
                SELECT DISTINCT bookingRoom.room.idRoom FROM BookingRoom bookingRoom
                WHERE bookingRoom.room.isDeleted = false
                    AND bookingRoom.checkIn <= :date
                    AND bookingRoom.checkOut > :date
                    AND bookingRoom.status.code <> 'CANCELED'
                    AND bookingRoom.booking.status.code <> 'CANCELED'
                """)
        List<Long> findBookedRoomIdsOn(@Param("date") LocalDate date);
}
