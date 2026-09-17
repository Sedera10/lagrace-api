package mg.lagrace.api.services;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import mg.lagrace.api.dto.customer.IdentificationDocumentRequest;
import mg.lagrace.api.dto.customer.NationalityRequest;
import mg.lagrace.api.dto.customer.SexRequest;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.IdentificationDocument;
import mg.lagrace.api.models.Nationality;
import mg.lagrace.api.models.Sex;
import mg.lagrace.api.repositories.IdentificationDocRepository;
import mg.lagrace.api.repositories.NationalityRepository;
import mg.lagrace.api.repositories.SexRepository;

@Service
public class CustomerConfigService {
    private final SexRepository sexRepository;
    private final NationalityRepository nationalityRepository;
    private final IdentificationDocRepository identificationDocRepository;

    public CustomerConfigService(SexRepository sexRepository, NationalityRepository nationalityRepository,
            IdentificationDocRepository identificationDocRepository) {
        this.sexRepository = sexRepository;
        this.nationalityRepository = nationalityRepository;
        this.identificationDocRepository = identificationDocRepository;
    }

    // =======================================
    // GESTION NATIONALITY
    // =======================================
    public List<Nationality> getNationalities() {
        return nationalityRepository.findAll();
    }

    public Nationality addNationality(NationalityRequest request) {
        validateNationalityRequest(request);
        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCountryCode(request.countryCode());
        validateCountryCode(normalizedCode);
        if (nationalityRepository.existsByName(normalizedName)
                || nationalityRepository.existsByCountryCode(normalizedCode)) {
            throw new DuplicateResourceException("La nationalité existe déjà.");
        }

        Nationality newNationality = new Nationality();
        newNationality.setName(normalizedName);
        newNationality.setCountryCode(normalizedCode);
        newNationality.setFlagEmoji(normalizeOptional(request.flagEmoji()));
        return nationalityRepository.save(newNationality);
    }

    public Nationality updateNationality(Long id, NationalityRequest request) {
        validateId(id, "la nationalité");
        validateNationalityRequest(request);
        Nationality existing = nationalityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nationalité introuvable avec l'id " + id));

        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCountryCode(request.countryCode());
        validateCountryCode(normalizedCode);
        if ((!existing.getName().equals(normalizedName) && nationalityRepository.existsByName(normalizedName))
                || (!existing.getCountryCode().equals(normalizedCode)
                        && nationalityRepository.existsByCountryCode(normalizedCode))) {
            throw new DuplicateResourceException("La nationalité existe déjà.");
        }

        existing.setName(normalizedName);
        existing.setCountryCode(normalizedCode);
        existing.setFlagEmoji(normalizeOptional(request.flagEmoji()));
        return nationalityRepository.save(existing);
    }

    public void deleteNationality(Long id) {
        validateId(id, "la nationalité");
        if (!nationalityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nationalité introuvable avec l'id " + id);
        }
        nationalityRepository.deleteById(id);
    }

    // =======================================
    // GESTION SEXE
    // =======================================
    public List<Sex> getSexes() {
        return sexRepository.findAll();
    }

    public Sex addSex(SexRequest request) {
        validateSexRequest(request);
        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCode(request.code());
        if (sexRepository.existsByName(normalizedName) || sexRepository.existsByCode(normalizedCode)) {
            throw new DuplicateResourceException("Le sexe existe déjà.");
        }

        return sexRepository.save(new Sex(normalizedName, normalizedCode));
    }

    public Sex updateSex(Long id, SexRequest request) {
        validateId(id, "le sexe");
        validateSexRequest(request);
        Sex existing = sexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sexe introuvable avec l'id " + id));

        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCode(request.code());
        if ((!existing.getName().equals(normalizedName) && sexRepository.existsByName(normalizedName))
                || (!existing.getCode().equals(normalizedCode) && sexRepository.existsByCode(normalizedCode))) {
            throw new DuplicateResourceException("Le sexe existe déjà.");
        }

        existing.setName(normalizedName);
        existing.setCode(normalizedCode);
        return sexRepository.save(existing);
    }

    public void deleteSex(Long id) {
        validateId(id, "le sexe");
        if (!sexRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sexe introuvable avec l'id " + id);
        }
        sexRepository.deleteById(id);
    }

    // =======================================
    // GESTION DOCUMENT D'IDENTIFICATION
    // =======================================
    public List<IdentificationDocument> getIdentificationDocuments() {
        return identificationDocRepository.findAll();
    }

    public IdentificationDocument addIdentificationDocument(IdentificationDocumentRequest request) {
        validateName(request == null ? null : request.name(), "du document d'identification");
        String normalizedName = normalizeName(request.name());
        if (identificationDocRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le document d'identification existe déjà.");
        }
        return identificationDocRepository.save(new IdentificationDocument(normalizedName));
    }

    public IdentificationDocument updateIdentificationDocument(Long id, IdentificationDocumentRequest request) {
        validateId(id, "le document d'identification");
        validateName(request == null ? null : request.name(), "du document d'identification");
        IdentificationDocument existing = identificationDocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document d'identification introuvable avec l'id " + id));
        String normalizedName = normalizeName(request.name());
        if (!existing.getName().equals(normalizedName) && identificationDocRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Le document d'identification existe déjà.");
        }
        existing.setName(normalizedName);
        return identificationDocRepository.save(existing);
    }

    public void deleteIdentificationDocument(Long id) {
        validateId(id, "le document d'identification");
        if (!identificationDocRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document d'identification introuvable avec l'id " + id);
        }
        identificationDocRepository.deleteById(id);
    }

    private void validateNationalityRequest(NationalityRequest request) {
        validateName(request == null ? null : request.name(), "de la nationalité");
        validateName(request == null ? null : request.countryCode(), "du pays");
    }

    private void validateSexRequest(SexRequest request) {
        validateName(request == null ? null : request.name(), "du sexe");
        validateName(request == null ? null : request.code(), "du code du sexe");
        if (request != null && request.code() != null && request.code().trim().length() != 1) {
            throw new ValidationException("Le code du sexe doit contenir exactement un caractère.");
        }
    }

    private void validateCountryCode(String code) {
        if (code.length() != 2 || !code.matches("[A-Z]{2}")) {
            throw new ValidationException("Le code du pays doit contenir exactement deux lettres.");
        }
    }

    private void validateName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Le nom " + field + " est obligatoire.");
        }
    }

    private void validateId(Long id, String resource) {
        if (id == null) {
            throw new ValidationException("L'identifiant de " + resource + " est obligatoire.");
        }
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizeCountryCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
