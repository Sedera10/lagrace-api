package mg.lagrace.api.catalog.space.application;

import mg.lagrace.api.booking.core.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import mg.lagrace.api.catalog.additionalservice.AdditionalService;
import mg.lagrace.api.catalog.space.Space;
import mg.lagrace.api.catalog.space.SpaceRate;
import mg.lagrace.api.catalog.space.SpaceRateRepository;
import mg.lagrace.api.catalog.space.SpaceRepository;
import mg.lagrace.api.catalog.space.dto.space.SpaceRateRequest;
import mg.lagrace.api.catalog.space.dto.space.SpaceRateResponse;
import mg.lagrace.api.catalog.space.dto.space.SpaceRequest;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.reference.Unit;
import mg.lagrace.api.catalog.additionalservice.persistence.AdditionalServiceRepository;
import mg.lagrace.api.booking.space.persistence.BookingSpaceRepository;
import mg.lagrace.api.reference.system.persistence.UnitRepository;

@Service
public class SpaceService {
    private final SpaceRepository spaceRepo;
    private final SpaceRateRepository spaceRateRepo;
    private final UnitRepository unitRepo;
    private final AdditionalServiceRepository additionalServices;
    private final BookingSpaceRepository bookingSpaces;

    public SpaceService(SpaceRepository spaceRepo, SpaceRateRepository spaceRateRepo, UnitRepository unitRepo,
            AdditionalServiceRepository additionalServices, BookingSpaceRepository bookingSpaces
    ) {
        this.spaceRepo = spaceRepo;
        this.spaceRateRepo = spaceRateRepo;
        this.unitRepo = unitRepo;
        this.additionalServices = additionalServices;
        this.bookingSpaces = bookingSpaces;
    }

    public boolean isAvailable(Long spaceId, LocalDate date) {
        if (spaceId == null) throw new ValidationException("L'espace est obligatoire.", "space.spaceId");
        if (date == null) throw new ValidationException("La date de l'événement est obligatoire.", "space.eventDate");
        if (!spaceRepo.existsById(spaceId)) throw new ResourceNotFoundException("Espace introuvable avec l'id : " + spaceId);
        return !bookingSpaces.existsActiveBookingForSpaceAndDate(spaceId, date);
    }

    public List<Space> getSpaces() {
        return spaceRepo.findAll();
    }

    public Space newSpace(SpaceRequest request) {
        return saveSpace(null, request);
    }

    public Space updateSpace(Long idSpace, SpaceRequest request) {
        return saveSpace(idSpace, request);
    }

    public Space saveSpace(Long idSpace, SpaceRequest request) {
        Space space = idSpace == null
                ? new Space()
                : spaceRepo.findById(idSpace)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Espace introuvable avec l'id : " + idSpace));
        validate(request);

        String name = request.name().trim();
        if (spaceRepo.existsByName(name)
                && (idSpace == null || !name.equals(space.getName()))) {
            throw new DuplicateResourceException("Nom déjà utilisé : " + request.name());
        }

        space.setName(name);
        space.setCapacity(request.capacity());
        space.setDescription(request.description() == null ? null : request.description().trim());

        return spaceRepo.save(space);
    }

    public void deleteSpace(Long idSpace) {
        Space space = spaceRepo.findById(idSpace)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Espace introuvable avec l'id : " + idSpace));
        spaceRepo.delete(space);
    }

    private void validate(SpaceRequest space) {
        if (space == null) {
            throw new ValidationException("Données vides ou non reçues");
        }
        if (space.name() == null || space.name().isBlank()) {
            throw new ValidationException("Le nom de l'espace est obligatoire.");
        }

        if (space.capacity() == null || space.capacity() < 1) {
            throw new ValidationException("Définir la capacité est obligatoire.");
        }

    }


    // ========= TARIFS =========
    @Transactional(readOnly = true)
    public List<SpaceRateResponse> getTarifs(Long idSpace) {
        spaceRepo.findById(idSpace)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Espace introuvable avec l'id : " + idSpace));

        List<SpaceRate> rates = spaceRateRepo.findBySpace_IdSpaceOrderByUnit_IdUnitAscMinPersonAscValidFromDesc(idSpace);
        return rates.stream()
            .map(SpaceRateResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SpaceRateResponse> currentRates(Long idSpace, LocalDate daty) {
        spaceRepo.findById(idSpace)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Espace introuvable avec l'id : " + idSpace));

        LocalDate newDate = (daty != null) ? daty : LocalDate.now();
        List<SpaceRate> rates = spaceRateRepo.findApplicableBySpace(idSpace, newDate);

        return rates.stream()
            .map(SpaceRateResponse::from)
            .toList();
    }


    @Transactional(readOnly = true)
    public List<SpaceRateResponse> othersRates(Long idSpace, LocalDate daty) {
        spaceRepo.findById(idSpace)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Espace introuvable avec l'id : " + idSpace));

        LocalDate newDate = (daty != null) ? daty : LocalDate.now();
        List<SpaceRate> allRates = spaceRateRepo.findBySpace_IdSpaceOrderByUnit_IdUnitAscMinPersonAscValidFromDesc(idSpace);
        List<SpaceRate> rates = spaceRateRepo.findApplicableBySpace(idSpace, newDate);

        Set<Long> rateIds = rates.stream()
            .map(SpaceRate::getIdRate)
            .collect(Collectors.toSet());

        List<SpaceRate> remainingRates = allRates.stream()
            .filter(rate -> !rateIds.contains(rate.getIdRate()))
            .toList();

        return remainingRates.stream()
            .map(SpaceRateResponse::from)
            .toList();
    }

    @Transactional
    public SpaceRateResponse updateTarif(Long idRate, SpaceRateRequest request) {
        if (idRate == null) {
            throw new ValidationException("L'identifiant du tarif est obligatoire.");
        }
        return saveTarif(idRate, request);
    }

    @Transactional
    public void deleteTarif(Long idRate) {
        if (idRate == null) {
            throw new ValidationException("L'identifiant du tarif est obligatoire.");
        }
        SpaceRate rate = spaceRateRepo.findById(idRate)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                "Tarif introuvable avec l'id : " + idRate));

        spaceRateRepo.delete(rate);
    }

    @Transactional
    public SpaceRateResponse addTarif(SpaceRateRequest request){
        return saveTarif(null, request);
    }

    private SpaceRateResponse saveTarif(Long idRate, SpaceRateRequest request) {
        validateTarif(request);
        Unit unit = unitRepo.findById(request.unitId()).orElseThrow(() -> new ResourceNotFoundException(
                                "Unité de mesure introuvable avec l'id : " + request.unitId()));
        Space space = spaceRepo.findById(request.spaceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Espace introuvable avec l'id : " + request.spaceId()));
                SpaceRate rate = idRate == null
                    ? new SpaceRate()
                    : spaceRateRepo.findById(idRate)
                        .orElseThrow(() -> new ResourceNotFoundException(
                            "Tarif introuvable avec l'id : " + idRate));
                rate.setSpace(space);
                rate.setUnit(unit);
                rate.setMinPerson(request.minPerson());
                rate.setMaxPerson(request.maxPerson());
                rate.setAmount(request.amount());
                rate.setValidFrom(request.validFrom());
                rate.setValidTo(request.validTo());

                return SpaceRateResponse.from(spaceRateRepo.save(rate));
    }

    private void validateTarif(SpaceRateRequest request) {
        if (request == null) {
            throw new ValidationException("Données vides ou non reçues");
        }

        if (request.spaceId() == null || request.unitId() == null) {
            throw new ValidationException("L'espace et l'unité concernés sont obligatoires.");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new ValidationException("Le montant du tarif est obligatoire et doit être positif.");
        }
        if (request.validFrom() == null) {
            throw new ValidationException("La date de début de validité est obligatoire.");
        }
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new ValidationException("La date de fin doit être postérieure à la date de début.");
        }
        if (request.minPerson() != null && request.minPerson() < 0) {
            throw new ValidationException("Le nombre minimal de personnes ne peut pas être négatif.");
        }
        if (request.maxPerson() != null && request.maxPerson() < 0) {
            throw new ValidationException("Le nombre maximal de personnes ne peut pas être négatif.");
        }
        if (request.minPerson() != null && request.maxPerson() != null
                && request.minPerson() > request.maxPerson()) {
            throw new ValidationException("Le minimum de personnes ne peut pas dépasser le maximum.");
        }
    }

    @Transactional(readOnly = true)
    public List<AdditionalService> getAdditionalServices(){
        List<AdditionalService> listes = additionalServices.findAll();
        return listes.stream()
            .filter(service -> service.getScope() != null
                && "SPACE".equals(service.getScope().getCode()))
            .toList();
    }
}
