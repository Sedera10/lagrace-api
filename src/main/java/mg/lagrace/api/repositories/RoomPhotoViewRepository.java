package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import mg.lagrace.api.models.views.RoomPhotoView;

public interface RoomPhotoViewRepository
        extends JpaRepository<RoomPhotoView, RoomPhotoView.RoomPhotoId> {

    List<RoomPhotoView> findByIdIdRoom(Integer idRoom);
}
