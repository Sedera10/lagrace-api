package mg.lagrace.api.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mg.lagrace.api.dto.ApiResponse;
import mg.lagrace.api.dto.photo.PhotoResponse;
import mg.lagrace.api.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.dto.room.RoomRequest;
import mg.lagrace.api.dto.room.RoomStatusHistoryResponse;
import mg.lagrace.api.dto.room.photo.RoomPhotoResponse;
import mg.lagrace.api.services.PhotoService;
import mg.lagrace.api.services.RoomService;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.models.views.RoomPhotoView;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;




@RestController
@RequestMapping("/rooms")
public class RoomController {
    private final RoomService roomService;
    private final PhotoService photoService;

    public RoomController(RoomService roomService, PhotoService photoService) {
        this.roomService = roomService;
        this.photoService = photoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomDetail>>> AllRooms() {
        List<RoomDetail> rooms = roomService.getRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms, "Liste des Chambres"));
    }

        @GetMapping("/{id}/status-history")
        public ResponseEntity<ApiResponse<List<RoomStatusHistoryResponse>>> getStatusHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit
        ) {
        return ResponseEntity.ok(ApiResponse.success(
            roomService.getHistoStatus(id, limit), "Historique des statuts récupéré."
        ));
        }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDetail>> AddRoom(@RequestBody RoomRequest request) {
        RoomDetail room = roomService.NewRoom(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(room, "Nouvelle chambre enregistrée."));
    }
    

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDetail>> updateRoomInfo(
            @PathVariable Long id,
            @RequestBody RoomRequest request) {
        RoomDetail updated = roomService.updateRoomDetail(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Chambre modifiée avec succès."));
    }

        @PatchMapping("/{roomId}/status")
        public ResponseEntity<ApiResponse<RoomDetail>> changeRoomStatus(
            @PathVariable Long roomId,
            @RequestParam("statusId") Long statusId
        ) {
        return ResponseEntity.ok(ApiResponse.success(
            roomService.changeRoomStatus(roomId, statusId),
            "Statut de la chambre mis à jour."
        ));
        }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveRoom(@PathVariable Long id) {
        roomService.archiveRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Chambre archivée avec succès."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteRoom(@PathVariable Long id) throws IOException {
        roomService.permanentlyDeleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Chambre supprimée définitivement."));
    }

    @GetMapping("{id}/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> roomPhotos(@PathVariable Long id) {
        List<PhotoResponse> photos = photoService.listByItem(PhotoItemType.ROOM, id);
        return ResponseEntity.ok(ApiResponse.success(photos, "Les photos (hors couverture) de la chambre " + id));
    }

    @PostMapping(value = "/{roomId}/photos",consumes = "multipart/form-data"
    )
    public ResponseEntity<ApiResponse<PhotoResponse>> uploadRoomPhoto(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String altText,
            @RequestParam(defaultValue = "false") boolean isCover
    ) throws IOException {

        PhotoResponse saved = photoService.upload(
               PhotoItemType.ROOM ,roomId, file, caption, altText, isCover
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(saved, "Photo de chambre ajoutée avec succès."));
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomPhoto(@PathVariable Long photoId) throws IOException {
        photoService.delete(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo de chambre supprimée avec succès."));
    }

    @PutMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<PhotoResponse>> updateRoomPhoto(
            @PathVariable Long photoId, @RequestBody UpdatePhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                photoService.update(photoId, request), "Photo de chambre modifiée avec succès."));
    }

    @PatchMapping("/{roomId}/photos/{photoId}/cover")
    public ResponseEntity<ApiResponse<Void>> setCoverPhoto(
            @PathVariable Long roomId,
            @PathVariable Long photoId
    ) {
        photoService.assertBelongsTo(photoId, PhotoItemType.ROOM, roomId);
        photoService.setCover(photoId);
        return ResponseEntity.ok(ApiResponse.success(null, "Photo définie comme couverture."));
    }

}
