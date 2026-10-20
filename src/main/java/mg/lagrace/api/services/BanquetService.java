package mg.lagrace.api.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.dto.banquet.BanquetRateRequest;
import mg.lagrace.api.dto.banquet.BanquetRateResponse;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.AdditionalService;
import mg.lagrace.api.models.BanquetMenuType;
import mg.lagrace.api.models.BanquetRate;
import mg.lagrace.api.repositories.AdditionalServiceRepository;
import mg.lagrace.api.repositories.BanquetMenuTypeRepository;
import mg.lagrace.api.repositories.BanquetRateRepository;

@Service
public class BanquetService {
	private final BanquetMenuTypeRepository menuTypeRepository;
	private final BanquetRateRepository rateRepository;
    private final AdditionalServiceRepository additionalServices;

	public BanquetService(BanquetMenuTypeRepository menuTypeRepository,
			BanquetRateRepository rateRepository,
            AdditionalServiceRepository additionalServices) {
		this.menuTypeRepository = menuTypeRepository;
		this.rateRepository = rateRepository;
        this.additionalServices = additionalServices;
	}

	@Transactional(readOnly = true)
	public List<BanquetMenuType> getMenuTypes() {
		return menuTypeRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<BanquetRateResponse> getRates() {
		return toResponses(rateRepository.findAllByOrderByMenuType_IdMenuTypeAscMinPersonsAscValidFromDesc());
	}

	@Transactional(readOnly = true)
	public List<BanquetRateResponse> getCurrentRates(LocalDate date) {
		LocalDate effectiveDate = date != null ? date : LocalDate.now();
		List<BanquetRate> applicableRates = menuTypeRepository.findAll().stream()
				.flatMap(menuType -> rateRepository.findApplicableByMenuType(
						menuType.getIdMenuType(),
						effectiveDate).stream())
				.toList();
		return toResponses(applicableRates);
	}

	@Transactional(readOnly = true)
	public List<BanquetRateResponse> getOtherRates(LocalDate date) {
		LocalDate effectiveDate = date != null ? date : LocalDate.now();
		Set<Long> currentIds = getCurrentRates(effectiveDate).stream()
				.map(BanquetRateResponse::idRate)
				.collect(Collectors.toSet());
		return toResponses(rateRepository.findAllByOrderByMenuType_IdMenuTypeAscMinPersonsAscValidFromDesc()
				.stream()
				.filter(rate -> !currentIds.contains(rate.getIdRate()))
				.toList());
	}

	@Transactional
	public BanquetRateResponse addRate(BanquetRateRequest request) {
		return saveRate(null, request);
	}

	@Transactional
	public BanquetRateResponse updateRate(Long idRate, BanquetRateRequest request) {
		if (idRate == null) {
			throw new ValidationException("L'identifiant du tarif est obligatoire.");
		}
		return saveRate(idRate, request);
	}

	@Transactional
	public void deleteRate(Long idRate) {
		if (idRate == null) {
			throw new ValidationException("L'identifiant du tarif est obligatoire.");
		}
		BanquetRate rate = rateRepository.findById(idRate)
				.orElseThrow(() -> new ResourceNotFoundException("Tarif banquet introuvable avec l'id : " + idRate));
		rateRepository.delete(rate);
	}

	private BanquetRateResponse saveRate(Long idRate, BanquetRateRequest request) {
		validateRate(request);
		BanquetMenuType menuType = menuTypeRepository.findById(request.menuTypeId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Type de menu banquet introuvable avec l'id : " + request.menuTypeId()));
		BanquetRate rate = idRate == null
				? new BanquetRate()
				: rateRepository.findById(idRate)
						.orElseThrow(() -> new ResourceNotFoundException(
								"Tarif banquet introuvable avec l'id : " + idRate));

		rate.setMenuType(menuType);
		rate.setMinPersons(request.minPersons());
		rate.setMaxPersons(request.maxPersons());
		rate.setAmount(request.amount());
		rate.setValidFrom(request.validFrom());
		rate.setValidTo(request.validTo());
		return BanquetRateResponse.from(rateRepository.save(rate));
	}

    @Transactional(readOnly = true)
    public List<AdditionalService> getAdditionalServices(){
        List<AdditionalService> listes = additionalServices.findAll();
        return listes.stream()
            .filter(service -> service.getScope() != null
                && "BANQUET".equals(service.getScope().getCode()))
            .toList();
    }

	private void validateRate(BanquetRateRequest request) {
		if (request == null) {
			throw new ValidationException("Les données du tarif banquet sont obligatoires.");
		}
		if (request.menuTypeId() == null) {
			throw new ValidationException("Le type de menu est obligatoire.");
		}
		if (request.minPersons() == null || request.minPersons() < 1) {
			throw new ValidationException("Le minimum de personnes doit être supérieur ou égal à 1.");
		}
		if (request.maxPersons() != null && request.maxPersons() < request.minPersons()) {
			throw new ValidationException("Le maximum de personnes ne peut pas être inférieur au minimum.");
		}
		if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) < 0) {
			throw new ValidationException("Le montant doit être positif ou nul.");
		}
		if (request.validFrom() == null) {
			throw new ValidationException("La date de début est obligatoire.");
		}
		if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
			throw new ValidationException("La date de fin doit être postérieure à la date de début.");
		}
	}

	private List<BanquetRateResponse> toResponses(List<BanquetRate> rates) {
		return rates.stream().map(BanquetRateResponse::from).toList();
	}
}
