package mg.lagrace.api.catalog.room.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.catalog.room.Equipment;
import mg.lagrace.api.catalog.room.RoomRate;
import mg.lagrace.api.catalog.room.RoomType;
import mg.lagrace.api.catalog.room.RoomTypeEquipment;
import mg.lagrace.api.catalog.room.RoomTypeEquipmentId;
import mg.lagrace.api.catalog.room.dto.room.rate.CreateRoomRateRequest;
import mg.lagrace.api.catalog.room.dto.room.rate.RoomRateResponse;
import mg.lagrace.api.catalog.room.dto.room.rate.UpdateRoomRateRequest;
import mg.lagrace.api.reference.Unit;
import mg.lagrace.api.catalog.room.persistence.RoomRateRepository.ApplicableNightRate;
import mg.lagrace.api.catalog.room.persistence.EquipmentRepository;
import mg.lagrace.api.catalog.room.persistence.RoomRateRepository;
import mg.lagrace.api.catalog.room.persistence.RoomTypeEquipmentRepository;
import mg.lagrace.api.catalog.room.persistence.RoomTypeRepository;
import mg.lagrace.api.reference.system.persistence.UnitRepository;

@Service
public class RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoomTypeEquipmentRepository roomTypeEquipmentRepository;
    private final RoomRateRepository roomRateRepository;
    private final UnitRepository unitRepository;

    private static final String ROOM_SERVICE = "ROOM";

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
        EquipmentRepository equipmentRepository, RoomTypeEquipmentRepository roomTypeEquipmentRepository,
        RoomRateRepository roomRateRepository, UnitRepository unitRepository) {

        this.roomTypeRepository = roomTypeRepository;
        this.equipmentRepository = equipmentRepository;
        this.roomTypeEquipmentRepository = roomTypeEquipmentRepository;
        this.roomRateRepository = roomRateRepository;
        this.unitRepository = unitRepository;
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
