package mg.lagrace.api.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.room.rate.CreateRoomRateRequest;
import mg.lagrace.api.dto.room.rate.RoomRateResponse;
import mg.lagrace.api.dto.room.rate.UpdateRoomRateRequest;
import mg.lagrace.api.models.Equipment;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.RoomRate;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.models.RoomTypeEquipment;
import mg.lagrace.api.models.RoomTypeEquipmentId;
import mg.lagrace.api.models.Unit;
import mg.lagrace.api.repositories.RoomRateRepository.ApplicableNightRate;
import mg.lagrace.api.repositories.EquipmentRepository;
import mg.lagrace.api.repositories.RoomPhotoRepository;
import mg.lagrace.api.repositories.RoomRateRepository;
import mg.lagrace.api.repositories.RoomTypeEquipmentRepository;
import mg.lagrace.api.repositories.RoomTypeRepository;
import mg.lagrace.api.repositories.UnitRepository;

@Service
public class RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final RoomPhotoRepository roomPhotoRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoomTypeEquipmentRepository roomTypeEquipmentRepository;
    private final RoomRateRepository roomRateRepository;
    private final UnitRepository unitRepository;

    private static final String ROOM_SERVICE = "ROOM";

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private final Cloudinary cloudinary;

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
        RoomPhotoRepository roomPhotoRepository, Cloudinary cloudinary,
        EquipmentRepository equipmentRepository, RoomTypeEquipmentRepository roomTypeEquipmentRepository,
        RoomRateRepository roomRateRepository, UnitRepository unitRepository) {

        this.roomTypeRepository = roomTypeRepository;
        this.roomPhotoRepository = roomPhotoRepository;
        this.cloudinary = cloudinary;
        this.equipmentRepository = equipmentRepository;
        this.roomTypeEquipmentRepository = roomTypeEquipmentRepository;
        this.roomRateRepository = roomRateRepository;
        this.unitRepository = unitRepository;
    }

    public List<RoomPhoto> getPhotoByType(Long idType) {
        return roomPhotoRepository.findByType_IdTypeOrderByIsCoverDescIdPhotoAsc(idType);
    }

    @Transactional
    public RoomPhoto updatePhotoCaption(Long photoId, String caption) {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("Photo introuvable"));
        photo.setCaption(normalizeCaption(caption));
        return roomPhotoRepository.save(photo);
    }

    @Transactional
    public RoomPhoto setCoverPhoto(Long typeId, Long photoId) {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo introuvable"));
        if (photo.getType() == null || !typeId.equals(photo.getType().getIdType())) {
            throw new IllegalArgumentException("Cette photo n'appartient pas au type de chambre");
        }

        if (Boolean.TRUE.equals(photo.getIsCover())) {
            photo.setIsCover(false);
        } else {
            roomPhotoRepository.findByType_IdTypeAndIsCoverTrue(typeId)
                .ifPresent(current -> current.setIsCover(false));
            photo.setIsCover(true);
        }
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

        Map<?, ?> uploadResult = null;

        try {
            uploadResult = cloudinary.uploader().upload(
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

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            if (secureUrl == null || publicId == null) {
                throw new IllegalStateException("Cloudinary n'a pas retourné les informations de l'image");
            }

            RoomPhoto photo = new RoomPhoto();
            photo.setUrl(secureUrl);
            photo.setPublicId(publicId);
            photo.setCaption(normalizeCaption(caption));
            photo.setIsCover(isCover);
            photo.setType(roomType);

            return roomPhotoRepository.save(photo);
        } catch (Exception exception) {
            if (uploadResult != null && uploadResult.get("public_id") != null) {
                cloudinary.uploader().destroy(
                        (String) uploadResult.get("public_id"),
                        ObjectUtils.asMap("resource_type", "image")
                );
            }
            throw exception;
        }
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

    //// DELETE PHOTO /////
    
    @Transactional
    public void deletePhoto(Long photoId) throws IOException {
        RoomPhoto photo = roomPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo introuvable"));

        Map<?, ?> result = cloudinary.uploader().destroy(
                photo.getPublicId(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "invalidate", true
                )
        );

        String resultStatus = (String) result.get("result");

        if (!"ok".equals(resultStatus) && !"not found".equals(resultStatus)) {
            throw new IllegalStateException(
                    "La suppression Cloudinary a échoué : " + resultStatus
            );
        }

        roomPhotoRepository.delete(photo);
    }

    // ============ EQUIPMENT =============//
    public List<RoomTypeEquipment> getEquipmentsByType(Long typeId) {
        roomTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalArgumentException("Type introuvable"));

        return roomTypeEquipmentRepository.findByType_IdTypeOrderByEquipment_NameAsc(typeId);
    }

    @Transactional
    public RoomTypeEquipment addOrUpdateEquipment(Long typeId, Long equipmentId, Integer quantity) {
        RoomType roomType = roomTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalArgumentException("Type introuvable"));

        Equipment equipment = equipmentRepository.findById(equipmentId)
            .orElseThrow(() -> new IllegalArgumentException("Équipement introuvable"));

        int safeQuantity = (quantity == null || quantity < 1) ? 1 : quantity;

        RoomTypeEquipment link = roomTypeEquipmentRepository
            .findByType_IdTypeAndEquipment_IdEquipment(typeId, equipmentId)
            .orElseGet(() -> {
                RoomTypeEquipment newLink = new RoomTypeEquipment();
                newLink.setId(new RoomTypeEquipmentId(typeId, equipmentId));
                newLink.setType(roomType);
                newLink.setEquipment(equipment);
                return newLink;
            });

        link.setQuantity(safeQuantity);

        return roomTypeEquipmentRepository.save(link);
    }

    @Transactional
    public void removeEquipmentFromType(Long typeId, Long equipmentId) {
        if (!roomTypeEquipmentRepository.existsByType_IdTypeAndEquipment_IdEquipment(typeId, equipmentId)) {
            throw new IllegalArgumentException("Association introuvable");
        }

        roomTypeEquipmentRepository.deleteByType_IdTypeAndEquipment_IdEquipment(typeId, equipmentId);
    }

    // ===================================
    // ============ TARIFS ===============
    // =================================== 
    @Transactional(readOnly = true)
    public List<RoomRateResponse> listTarifs(Long typeId, LocalDate date) {
        if (!roomTypeRepository.existsById(typeId)) {
            throw new IllegalArgumentException("Type de chambre introuvable");
        }

        LocalDate effectiveDate = date != null ? date : LocalDate.now();

        Set<Long> applicableIds = roomRateRepository.findApplicableByType(typeId, effectiveDate).stream()
                .map(RoomRate::getIdRate)
                .collect(Collectors.toSet());

        return roomRateRepository.findAllByType(typeId).stream()
                .map(r -> new RoomRateResponse(
                        r.getIdRate(), typeId, r.getUnit().getIdUnit(), r.getUnit().getName(),
                        r.getAmount(), r.getValidFrom(), r.getValidTo(),
                        applicableIds.contains(r.getIdRate())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, ApplicableNightRate> getApplicableNightRates(Collection<Long> typeIds, LocalDate date) {
        if (typeIds == null || typeIds.isEmpty()) {
            return Map.of();
        }
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return roomRateRepository.findApplicableNightRatesByTypes(typeIds, effectiveDate).stream()
                .collect(Collectors.toMap(ApplicableNightRate::getTypeId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public ApplicableNightRate getApplicableNightRate(Long typeId, LocalDate date) {
        return getApplicableNightRates(List.of(typeId), date).get(typeId);
    }

    @Transactional
    public RoomRateResponse createRate(Long typeId, CreateRoomRateRequest req) {
        RoomType type = roomTypeRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Type de chambre introuvable"));

        Unit unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unité introuvable"));
        if (!ROOM_SERVICE.equalsIgnoreCase(unit.getService())) {
            throw new IllegalArgumentException("Cette unité ne concerne pas les chambres");
        }

        checkRules(typeId, unit.getIdUnit(), req.validFrom(), req.validTo(), -1L);

        RoomRate rate = new RoomRate();
        rate.setType(type);
        rate.setUnit(unit);
        rate.setAmount(req.amount());
        rate.setValidFrom(req.validFrom());
        rate.setValidTo(req.validTo());

        return toResponse(roomRateRepository.save(rate), LocalDate.now());
    }

    @Transactional
    public RoomRateResponse updateRate(Long typeId, Long rateId, UpdateRoomRateRequest req) {
        RoomRate rate = findOrThrow(rateId);
        if (!rate.getType().getIdType().equals(typeId)) {
            throw new IllegalArgumentException("Ce tarif n'appartient pas à ce type de chambre");
        }

        checkRules(rate.getType().getIdType(), rate.getUnit().getIdUnit(),
                req.validFrom(), req.validTo(), rateId);

        rate.setAmount(req.amount());
        rate.setValidFrom(req.validFrom());
        rate.setValidTo(req.validTo());

        return toResponse(rate, LocalDate.now());   // entité managée : flush au commit
    }

    @Transactional
    public void deleteRate(Long rateId) {
        roomRateRepository.delete(findOrThrow(rateId));
    }

    @Transactional(readOnly = true)
    public RoomRateResponse getRate(Long rateId) {
        return toResponse(findOrThrow(rateId), LocalDate.now());
    }

    private void checkRules(Long typeId, Long unitId, LocalDate from, LocalDate to, Long excludeId) {
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }

        if (roomRateRepository.existsByType_IdTypeAndUnit_IdUnitAndValidFromAndIdRateNot(
                typeId, unitId, from, excludeId)) {
            throw new IllegalArgumentException("Un tarif commence déjà à cette date pour cette unité");
        }

        if (to == null) {
            if (roomRateRepository.existsByType_IdTypeAndUnit_IdUnitAndValidToIsNullAndIdRateNot(
                    typeId, unitId, excludeId)) {
                throw new IllegalArgumentException(
                        "Un tarif permanent existe déjà pour cette unité : fermez-le (date de fin) avant d'en créer un autre");
            }
        } else if (roomRateRepository.existsOverlappingPromo(typeId, unitId, from, to, excludeId)) {
            throw new IllegalArgumentException("Cette période chevauche une autre promotion de la même unité");
        }
    }
    
    private RoomRateResponse toResponse(RoomRate r, LocalDate date) {
        boolean applicable = roomRateRepository.findApplicableByType(r.getType().getIdType(), date)
                .stream().anyMatch(a -> a.getIdRate().equals(r.getIdRate()));
        return new RoomRateResponse(
                r.getIdRate(), r.getType().getIdType(), r.getUnit().getIdUnit(), r.getUnit().getName(),
                r.getAmount(), r.getValidFrom(), r.getValidTo(), applicable);
    }

    private RoomRate findOrThrow(Long id) {
        return roomRateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarif introuvable"));
    }
}
