package mg.lagrace.api.services;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.pool.PoolRateRequest;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.models.Pool;
import mg.lagrace.api.models.PoolRate;
import mg.lagrace.api.models.PoolService;
import mg.lagrace.api.models.Unit;
import mg.lagrace.api.models.views.PoolRateDetail;
import mg.lagrace.api.repositories.PoolRepository;
import mg.lagrace.api.repositories.PoolRateDetailRepository;
import mg.lagrace.api.repositories.PoolRateRepository;
import mg.lagrace.api.repositories.PoolServiceRepository;
import mg.lagrace.api.repositories.UnitRepository;

@Service
public class PoolManagmentService {
    private final PoolRateRepository rateRepo;
    private final PoolRateDetailRepository rateDetailRepo;
    private final PoolRepository poolRepo;
    private final PoolServiceRepository poolServiceRepo;
    private final UnitRepository unitRepo;

    public PoolManagmentService(
            PoolRateRepository rateRepo,
            PoolRateDetailRepository rateDetailRepo,
            PoolRepository poolRepo,
            PoolServiceRepository poolServiceRepo,
            UnitRepository unitRepo) {
        this.rateRepo = rateRepo;
        this.rateDetailRepo = rateDetailRepo;
        this.poolRepo = poolRepo;
        this.poolServiceRepo = poolServiceRepo;
        this.unitRepo = unitRepo;
    }

    // =========== LISTE - GET ============
    public List<PoolRateDetail> allRates(Long poodId){
        return rateDetailRepo.findByPoolId(poodId);
    }

    public List<PoolRateDetail> currentRates(Long poolId, LocalDate date) {
        LocalDate dateTest = (date != null) ? date : LocalDate.now();
        return rateDetailRepo.findApplicableRates(poolId, dateTest);
    }

    public List<PoolRateDetail> otherRates(Long poolId, LocalDate date) {
        LocalDate dateTest = (date != null) ? date : LocalDate.now();

        List<PoolRateDetail> currentRates =
                rateDetailRepo.findApplicableRates(poolId, dateTest);

        List<PoolRateDetail> allRates =
                rateDetailRepo.findByPoolId(poolId);

        Set<Long> currentIds = currentRates.stream()
                .map(PoolRateDetail::getIdRate)
                .collect(Collectors.toSet());

        return allRates.stream()
                .filter(rate -> !currentIds.contains(rate.getIdRate()))
                .toList();
    }

    // ======= CREATE / UPDATE =========
    @Transactional
    public PoolRateDetail SaveOrUpdatePoolRate(PoolRateRequest request){
        validateRequest(request);

        PoolRate rate = new PoolRate();
        apply(rate, request);
        PoolRate saved = rateRepo.saveAndFlush(rate);
        return detailOf(saved.getIdRate());
    }

    @Transactional
    public PoolRateDetail updatePoolRate(Long idRate, PoolRateRequest request) {
        validateRequest(request);

        if (idRate == null) {
            throw new ValidationException("L'identifiant du tarif est obligatoire.");
        }

        PoolRate rate = rateRepo.findById(idRate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarif piscine introuvable avec l'id " + idRate));

        apply(rate, request);
        PoolRate saved = rateRepo.saveAndFlush(rate);
        return detailOf(saved.getIdRate());
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
        if (Boolean.TRUE.equals(request.temporary()) && request.validTo() == null) {
            throw new ValidationException("Un tarif temporaire doit avoir une date de fin.");
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
        rate.setTemporary(Boolean.TRUE.equals(request.temporary()));
        rate.setValidFrom(request.validFrom());
        rate.setValidTo(request.validTo());
    }

    private PoolRateDetail detailOf(Long idRate) {
        return rateDetailRepo.findById(idRate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarif piscine introuvable après sauvegarde avec l'id " + idRate));
    }
}
