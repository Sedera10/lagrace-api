package mg.lagrace.api.catalog.room.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.room.RoomStatusHistory;

import java.util.List;
import org.springframework.data.domain.Pageable;


public interface RoomStatusHistoryRepository extends JpaRepository<RoomStatusHistory,Long> {
    List<RoomStatusHistory> findByRoomOrderByChangedAtDesc(Room room, Pageable pageable);
}
