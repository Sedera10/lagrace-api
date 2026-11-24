package mg.lagrace.api.catalog.room.persistence;

import mg.lagrace.api.catalog.room.Room;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.catalog.room.RoomDetail;

public interface RoomDetailRepository extends JpaRepository<RoomDetail,Long> {
    
}