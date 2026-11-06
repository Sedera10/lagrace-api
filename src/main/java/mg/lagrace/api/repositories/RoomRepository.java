package mg.lagrace.api.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mg.lagrace.api.models.Room;

public interface RoomRepository extends JpaRepository<Room,Long> {
    boolean existsByRefRoom(String refRoom);

    default List<Long> findAvailableRoomIds(LocalDate checkIn, LocalDate checkOut) {
        return findAvailableRoomIds(checkIn, checkOut, null);
    }

        @Query("""
                SELECT room.idRoom FROM Room room
                WHERE room.isDeleted = false
                    AND UPPER(TRIM(room.status.name)) = 'DISPONIBLE'
                    AND NOT EXISTS (
                            SELECT bookingRoom.idBookingRoom FROM BookingRoom bookingRoom
                            WHERE bookingRoom.room.idRoom = room.idRoom
                                AND bookingRoom.active = true
                                AND (:excludeBookingRoomId IS NULL OR bookingRoom.idBookingRoom <> :excludeBookingRoomId)
                                AND bookingRoom.checkIn < :checkOut
                                AND bookingRoom.checkOut > :checkIn
                    )
                ORDER BY room.idRoom
                """)
        List<Long> findAvailableRoomIds(@Param("checkIn") LocalDate checkIn,
                                                                        @Param("checkOut") LocalDate checkOut,
                                                                        @Param("excludeBookingRoomId") Long excludeBookingRoomId);
}
