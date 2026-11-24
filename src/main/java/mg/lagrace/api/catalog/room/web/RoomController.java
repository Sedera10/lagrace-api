package mg.lagrace.api.catalog.room.web;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.media.dto.photo.PhotoResponse;
import mg.lagrace.api.media.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.catalog.room.dto.room.RoomRequest;
import mg.lagrace.api.catalog.room.dto.room.RoomDetailResponse;
import mg.lagrace.api.catalog.room.dto.room.RoomSearchResponse;
import mg.lagrace.api.catalog.room.dto.room.RoomStatusHistoryResponse;
import mg.lagrace.api.media.PhotoItemType;
import mg.lagrace.api.media.application.PhotoService;
import mg.lagrace.api.catalog.room.application.RoomService;

import java.io.IOException;
import java.time.LocalDate;
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
    public ResponseEntity<ApiResponse<List<RoomDetailResponse>>> AllRooms(
            @RequestParam(required = false) LocalDate date) {
        List<RoomDetailResponse> rooms = roomService.getRooms(date);
        return ResponseEntity.ok(ApiResponse.success(rooms, "Liste des Chambres"));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<Long>>> getAvailableRooms(
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long excludeBookingRoomId
    ) {
        LocalDate resolvedFrom = from == null ? checkIn : from;
        LocalDate resolvedTo = to == null ? checkOut : to;
        List<Long> availableRoomIds = roomService.getAvailableRoomIds(resolvedFrom, resolvedTo, excludeBookingRoomId);
        return ResponseEntity.ok(ApiResponse.success(availableRoomIds, "Chambres disponibles."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoomSearchResponse>>> searchAvailableRooms(
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.success(
                roomService.searchAvailableRooms(checkIn, checkOut), "Chambres disponibles pour cette période."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDetailResponse>> AddRoom(@RequestBody RoomRequest request) {
        RoomDetailResponse room = roomService.NewRoom(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(room, "Nouvelle chambre enregistrée."));
    }
    

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> updateRoomInfo(
            @PathVariable Long id,
            @RequestBody RoomRequest request) {
        RoomDetailResponse updated = roomService.updateRoomDetail(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Chambre modifiée avec succès."));
    }

    // ================================ 
    // =========== STATUS =============
    // ================================ 

    @GetMapping("/{id}/status-history")
    public ResponseEntity<ApiResponse<List<RoomStatusHistoryResponse>>> getStatusHistory(
        @PathVariable Long id,
        @RequestParam(defaultValue = "10") int limit
    ) {
    return ResponseEntity.ok(ApiResponse.success(
        roomService.getHistoStatus(id, limit), "Historique des statuts récupéré."
    ));
    }


    @PatchMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> changeRoomStatus(
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

    // ================================ 
    // =========== STATUS =============
    // ================================ 

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
