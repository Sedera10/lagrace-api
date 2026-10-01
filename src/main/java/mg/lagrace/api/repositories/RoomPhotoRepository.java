package mg.lagrace.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mg.lagrace.api.models.RoomPhoto;

public interface RoomPhotoRepository extends JpaRepository<RoomPhoto,Long> {
    List<RoomPhoto> findByRoom_IdRoomOrderByIsCoverDescIdPhotoAsc(Long roomId);

    List<RoomPhoto> findByType_IdTypeOrderByIsCoverDescIdPhotoAsc(Long typeId);

    Optional<RoomPhoto> findByRoom_IdRoomAndIsCoverTrue(Long roomId);

    Optional<RoomPhoto> findByType_IdTypeAndIsCoverTrue(Long typeId);
}
