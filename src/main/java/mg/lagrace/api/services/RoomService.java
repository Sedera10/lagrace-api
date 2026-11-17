package mg.lagrace.api.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.transaction.Transactional;
import mg.lagrace.api.catalog.room.Room;
import mg.lagrace.api.catalog.room.RoomPhoto;
import mg.lagrace.api.catalog.room.RoomStatus;
import mg.lagrace.api.catalog.room.RoomStatusHistory;
import mg.lagrace.api.catalog.room.RoomType;
import mg.lagrace.api.dto.room.RoomRequest;
import mg.lagrace.api.dto.room.RoomSearchResponse;
import mg.lagrace.api.dto.room.RoomStatusHistoryResponse;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.views.RoomDetail;
import mg.lagrace.api.repositories.RoomRateRepository.ApplicableNightRate;
import mg.lagrace.api.repositories.RoomDetailRepository;
import mg.lagrace.api.repositories.RoomRepository;
import mg.lagrace.api.repositories.RoomStatusHistoryRepository;
import mg.lagrace.api.repositories.RoomStatusRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomDetailRepository roomDetailRepository;
    private final RoomStatusRepository roomStatusRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomStatusHistoryRepository roomStatusHistoryRepository;
    private final RoomTypeService roomTypeService;

    private final Cloudinary cloudinary;

    public RoomService(RoomRepository roomRepository, RoomDetailRepository roomDetailRepository,
            RoomStatusRepository roomStatusRepository, RoomTypeRepository roomTypeRepository,
            Cloudinary cloudinary, RoomStatusHistoryRepository roomStatusHistoryRepository,
            RoomTypeService roomTypeService) {
        this.roomRepository = roomRepository;
        this.roomDetailRepository = roomDetailRepository;
        this.roomStatusRepository = roomStatusRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.cloudinary = cloudinary;
        this.roomStatusHistoryRepository = roomStatusHistoryRepository;
        this.roomTypeService = roomTypeService;
    }

    // =======================================
    // ============== GENERALS ===============
    // =======================================
    public List<RoomDetail> getRooms() {
        return roomDetailRepository.findAll();
    }

    public List<Long> getAvailableRoomIds(LocalDate checkIn, LocalDate checkOut) {
        validateAvailabilityPeriod(checkIn, checkOut);
        return roomRepository.findAvailableRoomIds(checkIn, checkOut);
    }

    public List<Long> getAvailableRoomIds(LocalDate checkIn, LocalDate checkOut, Long excludeBookingRoomId) {
        validateAvailabilityPeriod(checkIn, checkOut);

        return roomRepository.findAvailableRoomIds(checkIn, checkOut, excludeBookingRoomId);
    }

    private void validateAvailabilityPeriod(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null)
            throw new ValidationException("Les dates d'arrivée et de départ sont obligatoires.");
        if (!checkOut.isAfter(checkIn))
            throw new ValidationException("La date de départ doit être après la date d'arrivée.");
    }

    @Transactional
    public List<RoomSearchResponse> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        validateSearchPeriod(checkIn, checkOut);
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ValidationException("La date d'arrivée ne peut pas être dans le passé.");
        }

        Set<Long> availableIds = Set.copyOf(roomRepository.findAvailableRoomIds(checkIn, checkOut, null));
        List<RoomDetail> availableRooms = roomDetailRepository.findAll().stream()
            .filter(room -> room.getIdRoom() != null && availableIds.contains(room.getIdRoom().longValue()))
            .toList();
        List<Long> typeIds = availableRooms.stream()
            .map(RoomDetail::getTypeId)
            .filter(typeId -> typeId != null)
            .map(Integer::longValue)
            .distinct()
            .toList();
        Map<Long, ApplicableNightRate> ratesByType = roomTypeService.getApplicableNightRates(typeIds, checkIn);

        return availableRooms.stream()
            .map(room -> RoomSearchResponse.from(
                room,
                room.getTypeId() == null ? null : ratesByType.get(room.getTypeId().longValue()),
                checkIn))
            .toList();
    }

    private void validateSearchPeriod(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Les dates d'arrivée et de départ sont obligatoires.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("La date de départ doit être après la date d'arrivée.");
        }
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

    // =======================================
    // ======== STATUS & HISTORIQUES =========
    // =======================================
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

}
