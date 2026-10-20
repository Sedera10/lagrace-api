package mg.lagrace.api.services;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.pool.PoolRateRequest;
import mg.lagrace.api.dto.pool.PoolRateResponse;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.Pool;
import mg.lagrace.api.models.PoolRate;
import mg.lagrace.api.models.PoolService;
import mg.lagrace.api.models.Space;
import mg.lagrace.api.models.Unit;
import mg.lagrace.api.repositories.PoolRepository;
import mg.lagrace.api.repositories.PoolRateRepository;
import mg.lagrace.api.repositories.PoolServiceRepository;
import mg.lagrace.api.repositories.UnitRepository;

@Service
public class PoolManagmentService {
    private final PoolRateRepository rateRepo;
    private final PoolRepository poolRepo;
    private final PoolServiceRepository poolServiceRepo;
    private final UnitRepository unitRepo;

    public PoolManagmentService(
            PoolRateRepository rateRepo,
            PoolRepository poolRepo,
            PoolServiceRepository poolServiceRepo,
            UnitRepository unitRepo) {
        this.rateRepo = rateRepo;
        this.poolRepo = poolRepo;
        this.poolServiceRepo = poolServiceRepo;
        this.unitRepo = unitRepo;
    }

    public List<Pool> getPools() {
        return poolRepo.findAll();
    }

    // =========== LISTE - GET (Tarifs) ============
    @Transactional(readOnly = true)
    public List<PoolRateResponse> allRates(Long poolId){
        return rateRepo.findByPool_IdPoolOrderByService_IdServiceAscUnit_IdUnitAscValidFromDesc(poolId)
                .stream()
                .map(PoolRateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PoolRateResponse> currentRates(Long poolId, LocalDate date) {
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        List<PoolRate> currentRates = new ArrayList<>();
        for (PoolService service : poolServiceRepo.findAll()) {
            currentRates.addAll(rateRepo.findApplicableByPoolAndService(
                    poolId,
                    service.getIdService(),
                    effectiveDate));
        }
        return currentRates.stream()
                .map(PoolRateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PoolRateResponse> otherRates(Long poolId, LocalDate date) {
        Set<Long> currentIds = currentRates(poolId, date).stream()
                .map(PoolRateResponse::idRate)
                .collect(Collectors.toSet());

        return rateRepo.findByPool_IdPoolOrderByService_IdServiceAscUnit_IdUnitAscValidFromDesc(poolId)
                .stream()
                .filter(rate -> !currentIds.contains(rate.getIdRate()))
                .map(PoolRateResponse::from)
                .toList();
    }

    // ======= CREATE / UPDATE =========
    @Transactional
    public PoolRateResponse SaveOrUpdatePoolRate(PoolRateRequest request) {
        validateRequest(request);
        PoolRate rate = new PoolRate();
        apply(rate, request);
        return PoolRateResponse.from(rateRepo.saveAndFlush(rate));
    }

    @Transactional
    public PoolRateResponse updatePoolRate(Long idRate, PoolRateRequest request) {
        validateRequest(request);

        if (idRate == null) {
            throw new ValidationException("L'identifiant du tarif est obligatoire.");
        }

        PoolRate rate = rateRepo.findById(idRate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarif piscine introuvable avec l'id " + idRate));
        apply(rate, request);
        return PoolRateResponse.from(rateRepo.saveAndFlush(rate));
    }

    @Transactional
    public void deletePoolRate(Long idRate) {
        if (idRate == null) {
            throw new ValidationException("L'identifiant du tarif est obligatoire.");
        }

        PoolRate rate = rateRepo.findById(idRate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarif piscine introuvable avec l'id " + idRate));

        rateRepo.delete(rate);
    }

    private void validateRequest(PoolRateRequest request) {
        if (request == null) {
            throw new ValidationException("Les données du tarif sont obligatoires.");
        }
        if (request.poolId() == null) {
            throw new ValidationException("La piscine est obligatoire.");
        }
        if (request.serviceId() == null || request.unitId() == null) {
            throw new ValidationException("Le service et l'unité sont obligatoires.");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Le montant doit être positif ou nul.");
        }
        if (request.validFrom() == null) {
            throw new ValidationException("La date de début est obligatoire.");
        }
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new ValidationException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (request.minAge() != null && request.minAge() < 0) {
            throw new ValidationException("L'âge minimum ne peut pas être négatif.");
        }
        if (request.maxAge() != null && request.maxAge() < 0) {
            throw new ValidationException("L'âge maximum ne peut pas être négatif.");
        }
        if (request.minAge() != null && request.maxAge() != null
                && request.minAge() > request.maxAge()) {
            throw new ValidationException("L'âge minimum ne peut pas dépasser l'âge maximum.");
        }
        boolean ageRate = request.minAge() != null || request.maxAge() != null;
        boolean mealRate = request.mealIncluded() != null;
        if (ageRate == mealRate) {
            throw new ValidationException(
                    "Un tarif par âge doit avoir les âges renseignés et aucun repas ; "
                            + "un tarif repas doit avoir le champ repas renseigné et aucun âge.");
        }
    }

    private void apply(PoolRate rate, PoolRateRequest request) {
        Pool pool = poolRepo.findById(request.poolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Piscine introuvable avec l'id " + request.poolId()));
        PoolService service = poolServiceRepo.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service piscine introuvable avec l'id " + request.serviceId()));
        Unit unit = unitRepo.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unité introuvable avec l'id " + request.unitId()));

        rate.setPool(pool);
        rate.setService(service);
        rate.setUnit(unit);
        rate.setIsMealIncluded(request.mealIncluded());
        rate.setMinAge(request.minAge());
        rate.setMaxAge(request.maxAge());
        rate.setAmount(request.amount());
        rate.setValidFrom(request.validFrom());
        rate.setValidTo(request.validTo());
    }

}
