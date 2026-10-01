package mg.lagrace.api.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.transaction.Transactional;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRow;
import mg.lagrace.api.dto.room.rate.RoomTypeRateRowRequest;
import mg.lagrace.api.models.Equipment;
import mg.lagrace.api.models.RoomPhoto;
import mg.lagrace.api.models.RoomRate;
import mg.lagrace.api.models.RoomType;
import mg.lagrace.api.models.RoomTypeEquipment;
import mg.lagrace.api.models.RoomTypeEquipmentId;
import mg.lagrace.api.models.Unit;
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
    private static final String NIGHT_UNIT_NAME = "Nuit";
    private static final String DAY_UNIT_NAME = "Jour";
    private static final String HOUR_UNIT_NAME = "Heure";

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private final Cloudinary cloudinary;

    private record PeriodKey(LocalDate validFrom, LocalDate validTo) {}

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

    // ============ TARIFS =============//
    public List<RoomTypeRateRow> getRateRowsByType(Long typeId) {
        roomTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalArgumentException("Type introuvable"));

        List<RoomRate> rates = roomRateRepository.findByType_IdTypeAndUnit_Service(typeId, ROOM_SERVICE);
        Map<PeriodKey, Map<Long, BigDecimal>> grouped = new HashMap<>();

        for (RoomRate rate : rates) {
            PeriodKey key = new PeriodKey(rate.getValidFrom(), rate.getValidTo());
            grouped.computeIfAbsent(key, ignored -> new HashMap<>())
                .put(rate.getUnit().getIdUnit(), rate.getAmount());
        }

        Long nightUnitId = getUnitIdByName(NIGHT_UNIT_NAME);
        Long dayUnitId = getUnitIdByName(DAY_UNIT_NAME);
        Long hourUnitId = getUnitIdByName(HOUR_UNIT_NAME);

        return grouped.entrySet().stream()
            .map(entry -> {
                LocalDate validFrom = entry.getKey().validFrom();
                LocalDate validTo = entry.getKey().validTo();
                Map<Long, BigDecimal> values = entry.getValue();

                return new RoomTypeRateRow(
                    validFrom,
                    validTo,
                    values.get(nightUnitId),
                    values.get(dayUnitId),
                    values.get(hourUnitId)
                );
            })
            .sorted(Comparator.comparing(RoomTypeRateRow::validFrom))
            .toList();
    }

    @Transactional
    public RoomRate upsertRateForType(Long typeId, Long unitId, LocalDate validFrom, LocalDate validTo, BigDecimal amount) {
        if (typeId == null) {
            throw new IllegalArgumentException("Le type est requis");
        }
        if (unitId == null) {
            throw new IllegalArgumentException("L'unité est requise");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("La date de début est requise");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant doit être positif ou nul");
        }
        if (validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }

        RoomType roomType = roomTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalArgumentException("Type introuvable"));

        Unit unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new IllegalArgumentException("Unité introuvable"));

        if (!ROOM_SERVICE.equalsIgnoreCase(unit.getService())) {
            throw new IllegalArgumentException("Cette unité ne concerne pas les chambres");
        }

        RoomRate rate = roomRateRepository
            .findByType_IdTypeAndUnit_IdUnitAndValidFrom(typeId, unitId, validFrom)
            .orElseGet(() -> {
                RoomRate newRate = new RoomRate();
                newRate.setType(roomType);
                newRate.setUnit(unit);
                newRate.setValidFrom(validFrom);
                return newRate;
            });

        rate.setValidTo(validTo);
        rate.setAmount(amount);

        return roomRateRepository.save(rate);
    }

    @Transactional
    public RoomTypeRateRow upsertRateRowForType(Long typeId, RoomTypeRateRowRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Les données du tarif sont requises");
        }

        Long nightUnitId = getUnitIdByName(NIGHT_UNIT_NAME);
        Long dayUnitId = getUnitIdByName(DAY_UNIT_NAME);
        Long hourUnitId = getUnitIdByName(HOUR_UNIT_NAME);

        if (request.previousValidFrom() != null && !request.previousValidFrom().equals(request.validFrom())) {
            moveRatePeriod(typeId, nightUnitId, request, request.nightAmount());
            moveRatePeriod(typeId, dayUnitId, request, request.dayAmount());
            moveRatePeriod(typeId, hourUnitId, request, request.hourAmount());
        } else {
            upsertRateForType(typeId, nightUnitId, request.validFrom(), request.validTo(), request.nightAmount());
            upsertRateForType(typeId, dayUnitId, request.validFrom(), request.validTo(), request.dayAmount());
            upsertRateForType(typeId, hourUnitId, request.validFrom(), request.validTo(), request.hourAmount());
        }

        return new RoomTypeRateRow(
            request.validFrom(),
            request.validTo(),
            request.nightAmount(),
            request.dayAmount(),
            request.hourAmount()
        );
    }

    @Transactional
    public void deleteRateRowForType(Long typeId, LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null) {
            throw new IllegalArgumentException("La date de début est requise");
        }

        List<RoomRate> rates = roomRateRepository.findByType_IdTypeAndUnit_Service(typeId, ROOM_SERVICE).stream()
            .filter(rate -> validFrom.equals(rate.getValidFrom()) && Objects.equals(validTo, rate.getValidTo()))
            .toList();

        if (rates.isEmpty()) {
            throw new IllegalArgumentException("Période tarifaire introuvable");
        }

        roomRateRepository.deleteAll(rates);
    }

    private void moveRatePeriod(Long typeId, Long unitId, RoomTypeRateRowRequest request, BigDecimal amount) {
        RoomRate rate = roomRateRepository
            .findByType_IdTypeAndUnit_IdUnitAndValidFrom(typeId, unitId, request.previousValidFrom())
            .orElseThrow(() -> new IllegalArgumentException("Tarif d'origine introuvable"));

        rate.setValidFrom(request.validFrom());
        rate.setValidTo(request.validTo());
        rate.setAmount(amount);
        roomRateRepository.save(rate);
    }

    private Long getUnitIdByName(String unitName) {
        return unitRepository.findByService(ROOM_SERVICE).stream()
            .filter(unit -> unitName.equalsIgnoreCase(unit.getName()))
            .map(Unit::getIdUnit)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unité chambre introuvable : " + unitName));
    }
}
