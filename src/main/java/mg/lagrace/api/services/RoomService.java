package mg.lagrace.api.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.transaction.Transactional;
import mg.lagrace.api.dto.room.RoomRequest;
import mg.lagrace.api.dto.room.RoomStatusHistoryResponse;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Room;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.RoomStatus;
import mg.lagrace.api.models.RoomStatusHistory;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.models.User;
import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.models.views.RoomPhotoView;
import mg.lagrace.api.repositories.RoomDetailRepository;
import mg.lagrace.api.repositories.RoomPhotoRepository;
import mg.lagrace.api.repositories.RoomPhotoViewRepository;
import mg.lagrace.api.repositories.RoomRepository;
import mg.lagrace.api.repositories.RoomStatusHistoryRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;
import mg.lagrace.api.repositories.UserRepository;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomDetailRepository roomDetailRepository;
    private final RoomStatusRepository roomStatusRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomPhotoViewRepository roomPhotoViewRepository; 
    private final RoomPhotoRepository roomPhotoRepository; 
    private final RoomStatusHistoryRepository roomStatusHistoryRepository;


    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private final Cloudinary cloudinary;

    public RoomService(RoomRepository roomRepository, RoomDetailRepository roomDetailRepository,
            RoomStatusRepository roomStatusRepository, RoomTypeRepository roomTypeRepository,
            RoomPhotoViewRepository roomPhotoViewRepository, RoomPhotoRepository roomPhotoRepository,
            Cloudinary cloudinary, RoomStatusHistoryRepository roomStatusHistoryRepository) {
        this.roomRepository = roomRepository;
        this.roomDetailRepository = roomDetailRepository;
        this.roomStatusRepository = roomStatusRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomPhotoViewRepository = roomPhotoViewRepository;
        this.roomPhotoRepository = roomPhotoRepository;
        this.cloudinary = cloudinary;
        this.roomStatusHistoryRepository = roomStatusHistoryRepository;
    }

    public List<RoomDetail> getRooms() {
        return roomDetailRepository.findAll();
    }

    public RoomDetail NewRoom(RoomRequest room) {
        validateCreation(room);

        if(roomRepository.existsByRefRoom(room.refRoom().trim())) {
            throw new DuplicateResourceException("Référence déjà utilisée : " + room.refRoom());
        }
        RoomType newType = roomTypeRepository.findById(room.typeId().longValue())
            .orElseThrow(() -> new ResourceNotFoundException("Type de chambre introuvable avec l'id " + room.typeId()));
        RoomStatus newStatus = roomStatusRepository.findById(room.statusId().longValue())
            .orElseThrow(() -> new ResourceNotFoundException("Statut de chambre introuvable avec l'id " + room.statusId()));
        String normalizedRef = room.refRoom().trim();
        String normalizedZone = room.zone() == null ? "" : room.zone().trim();
        
        Room newRoom = new Room();
        newRoom.setRefRoom(normalizedRef);
        newRoom.setZone(normalizedZone);
        newRoom.setFloor(room.floor());
        newRoom.setType(newType);
        newRoom.setStatus(newStatus);
        roomRepository.save(newRoom);

        return roomDetailRepository.findById(newRoom.getIdRoom()).orElse(null);
    }

    private void validateCreation(RoomRequest room) {
        if (room == null) {
            throw new ValidationException("Données vides ou non reçues");
        }
        if (room.refRoom() == null || room.refRoom().isBlank()) {
            throw new ValidationException("La référence de la chambre est obligatoire.");
        }
        if (room.typeId() == null) {
            throw new ValidationException("Le type de la chambre est obligatoire.");
        }
        if (room.statusId() == null) {
            throw new ValidationException("Le statut de la chambre est obligatoire.");
        }
        if (room.floor() == null || room.floor() < 0) {
            throw new ValidationException("L'étage doit être supérieur ou égal à zéro.");
        }
    }
    public RoomDetail updateRoomDetail(Long id, RoomRequest room) {
        if (id == null) {
            throw new ValidationException("L'identifiant de la chambre est obligatoire.");
        }
        if (room == null || room.refRoom() == null || room.statusId() == null) {
            throw new ValidationException("La référence et le statut de la chambre sont obligatoires.");
        }
        if (room.typeId() == null) {
            throw new ValidationException("Le type de la chambre est obligatoire.");
        }
        Room existing = roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id " + id));
        RoomType newType = roomTypeRepository.findById(room.typeId().longValue())
            .orElseThrow(() -> new ResourceNotFoundException("Type de chambre introuvable avec l'id " + room.typeId()));
        RoomStatus newStatus = roomStatusRepository.findById(room.statusId().longValue())
            .orElseThrow(() -> new ResourceNotFoundException("Statut de chambre introuvable avec l'id " + room.statusId()));
        String normalizedRef = room.refRoom().trim();
        String normalizedZone = room.zone() == null ? "" : room.zone().trim();
        if (normalizedRef.isEmpty()) {
            throw new ValidationException("La référence de la chambre est obligatoire.");
        }
        if (!existing.getRefRoom().equals(normalizedRef)
                && roomRepository.existsByRefRoom(normalizedRef)) {
            throw new DuplicateResourceException("La référence '" + normalizedRef + "' existe déjà.");
        }
        existing.setRefRoom(normalizedRef);
        existing.setZone(normalizedZone);
        existing.setFloor(room.floor());
        existing.setType(newType);
        existing.setStatus(newStatus);
        roomRepository.save(existing);

        return roomDetailRepository.findById(existing.getIdRoom()).orElse(null);
    }

    @Transactional
    public RoomDetail changeRoomStatus(Long roomId, Long statusId) {
        if (roomId == null || statusId == null) {
            throw new ValidationException("La chambre et le statut sont obligatoires.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id " + roomId));
        RoomStatus status = roomStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Statut introuvable avec l'id " + statusId));

        if (!statusId.equals(room.getStatus().getIdStatus())) {
            room.setStatus(status);
            roomRepository.save(room);

            RoomStatusHistory history = new RoomStatusHistory();
            history.setRoom(room);
            history.setStatus(status);
            roomStatusHistoryRepository.save(history);
        }

        return roomDetailRepository.findById(roomId).orElse(null);
    }

    @Transactional
    public void archiveRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id " + roomId));
        room.setIsDeleted(true);
        room.setDeletedAt(OffsetDateTime.now());
        roomRepository.save(room);
    }

    @Transactional
    public void permanentlyDeleteRoom(Long roomId) throws IOException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id " + roomId));

        for (RoomPhoto photo : room.getPhotos()) {
            if (photo.getPublicId() == null || photo.getPublicId().isBlank()) continue;
            cloudinary.uploader().destroy(
                    photo.getPublicId(),
                    ObjectUtils.asMap("resource_type", "image", "invalidate", true)
            );
        }
        roomRepository.delete(room);
    }

    public List<RoomPhotoView> getRoomPhotos(Integer roomId) {
        return roomPhotoViewRepository.findByIdIdRoom(roomId);
    }

    @Transactional
    public void deletePhoto(Long photoId) throws IOException {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo introuvable"));

        Map<?, ?> result = cloudinary.uploader().destroy(
                photo.getPublicId(),
                ObjectUtils.asMap("resource_type", "image", "invalidate", true)
        );
        String resultStatus = (String) result.get("result");
        if (!"ok".equals(resultStatus) && !"not found".equals(resultStatus)) {
            throw new IllegalStateException("La suppression Cloudinary a échoué : " + resultStatus);
        }

        roomPhotoRepository.delete(photo);
    }

    @Transactional
    public RoomPhoto setCoverPhoto(Long roomId, Long photoId) {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo introuvable"));
        if (photo.getRoom() == null || !roomId.equals(photo.getRoom().getIdRoom())) {
            throw new IllegalArgumentException("Cette photo n'appartient pas à la chambre");
        }

        if (Boolean.TRUE.equals(photo.getIsCover())) {
            photo.setIsCover(false);
        } else {
            roomPhotoRepository.findByRoom_IdRoomAndIsCoverTrue(roomId)
                .ifPresent(current -> current.setIsCover(false));
            photo.setIsCover(true);
        }
        return roomPhotoRepository.save(photo);
    }

    // ================================
    // ========     PHOTOS     ========
    // ================================
    @Transactional
    public RoomPhoto uploadForRoom(
            Long roomId,
            MultipartFile file,
            String caption,
            boolean isCover
    ) throws IOException {

        validateImage(file);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chambre introuvable"));

        if (isCover) {
            roomPhotoRepository.findByRoom_IdRoomAndIsCoverTrue(roomId)
                    .ifPresent(oldCover -> oldCover.setIsCover(false));
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "hotel/rooms/" + roomId,
                        "public_id", UUID.randomUUID().toString(),
                        "overwrite", false,
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );

        String secureUrl = (String) uploadResult.get("secure_url");
        String cloudinaryPublicId = (String) uploadResult.get("public_id");

        if (secureUrl == null || cloudinaryPublicId == null) {
            throw new IllegalStateException(
                    "Cloudinary n'a pas retourné les informations de l'image"
            );
        }

        RoomPhoto photo = new RoomPhoto();
        photo.setUrl(secureUrl);
        photo.setPublicId(cloudinaryPublicId);
        photo.setCaption(normalizeCaption(caption));
        photo.setIsCover(isCover);
        photo.setRoom(room);
        photo.setType(null);

        return roomPhotoRepository.save(photo);
    }

    @Transactional
    public RoomPhoto uploadForRoomType(
            Long typeId,
            MultipartFile file,
            String caption,
            boolean isCover
    ) throws IOException {

        validateImage(file);

        RoomType roomType = roomTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Type de chambre introuvable"));

        if (isCover) {
            roomPhotoRepository.findByType_IdTypeAndIsCoverTrue(typeId)
                    .ifPresent(oldCover -> oldCover.setIsCover(false));
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "hotel/room-types/" + typeId,
                        "public_id", UUID.randomUUID().toString(),
                        "overwrite", false,
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );

        RoomPhoto photo = new RoomPhoto();
        photo.setUrl((String) uploadResult.get("secure_url"));
        photo.setPublicId((String) uploadResult.get("public_id"));
        photo.setCaption(normalizeCaption(caption));
        photo.setIsCover(isCover);
        photo.setRoom(null);
        photo.setType(roomType);

        return roomPhotoRepository.save(photo);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Veuillez sélectionner une image");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "La taille maximale autorisée est de 5 Mo"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Formats acceptés : JPEG, PNG et WebP"
            );
        }
    }

    private String normalizeCaption(String caption) {
        if (caption == null || caption.isBlank()) {
            return null;
        }

        String normalized = caption.trim();

        if (normalized.length() > 255) {
            throw new IllegalArgumentException(
                    "La légende ne doit pas dépasser 255 caractères"
            );
        }

        return normalized;
    }

    // ========== STATUS & HISTORIQUES ============= //
    public List<RoomStatusHistoryResponse> getHistoStatus(Long idRoom, int limit) {
        if(idRoom == null) {
            throw new ValidationException("Identifiant de la chambre non réçu.");
        }
        Room room = roomRepository.findById(idRoom)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre introuvable avec l'id " + idRoom));
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        return roomStatusHistoryRepository
            .findByRoomOrderByChangedAtDesc(room, PageRequest.of(0, safeLimit))
            .stream()
            .map(RoomStatusHistoryResponse::from)
            .toList();
    }

}
