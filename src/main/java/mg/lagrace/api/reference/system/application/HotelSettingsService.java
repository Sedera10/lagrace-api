package mg.lagrace.api.reference.system.application;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.reference.HotelSettings;
import mg.lagrace.api.reference.system.dto.HotelSettingsRequest;
import mg.lagrace.api.reference.system.persistence.HotelSettingsRepository;

@Service
public class HotelSettingsService {
    private static final long SETTINGS_ID = 1L;

    private final HotelSettingsRepository settings;
    private final Clock clock;

    public HotelSettingsService(HotelSettingsRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HotelSettings get() {
        return settings.findById(SETTINGS_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètres de l'hôtel introuvables."));
    }

    @Transactional
    public HotelSettings update(HotelSettingsRequest request) {
        HotelSettings current = get();
        current.setLegalName(request.legalName().trim());
        current.setAddress(clean(request.address()));
        current.setPhone(clean(request.phone()));
        current.setEmail(clean(request.email()));
        current.setNif(clean(request.nif()));
        current.setStat(clean(request.stat()));
        current.setPaymentDetails(clean(request.paymentDetails()));
        current.setInvoiceFooter(clean(request.invoiceFooter()));
        current.setUpdatedAt(OffsetDateTime.now(clock));
        return settings.save(current);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
