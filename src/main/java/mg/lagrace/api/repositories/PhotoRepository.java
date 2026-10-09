package mg.lagrace.api.repositories;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import mg.lagrace.api.models.Photo;
import mg.lagrace.api.models.enums.PhotoItemType;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByItemTypeAndItemIdOrderByPositionAsc(PhotoItemType type, Long itemId);

    List<Photo> findByItemType(PhotoItemType type, Sort sort);

    @Query("SELECT COALESCE(MAX(p.position), -1) + 1 FROM Photo p " +
           "WHERE p.itemType = :type AND p.itemId = :itemId")
    int nextPosition(PhotoItemType type, Long itemId);

    // flush + clear : l'ancienne couverture doit être décochée en base
    // AVANT d'en poser une nouvelle (index unique partiel)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Photo p SET p.cover = false " +
           "WHERE p.itemType = :type AND p.itemId = :itemId AND p.cover = true")
    void clearCover(PhotoItemType type, Long itemId);
}
