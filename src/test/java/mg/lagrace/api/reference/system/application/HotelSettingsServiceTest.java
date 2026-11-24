package mg.lagrace.api.reference.system.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.lagrace.api.reference.HotelSettings;
import mg.lagrace.api.reference.system.dto.HotelSettingsRequest;
import mg.lagrace.api.reference.system.persistence.HotelSettingsRepository;

@ExtendWith(MockitoExtension.class)
class HotelSettingsServiceTest {
    @Mock private HotelSettingsRepository settings;

    private HotelSettingsService service;
    private HotelSettings existing;

    @BeforeEach
    void setUp() {
        service = new HotelSettingsService(settings,
                Clock.fixed(Instant.parse("2026-11-24T05:00:00Z"), ZoneId.of("Indian/Antananarivo")));
        existing = new HotelSettings();
        when(settings.findById(1L)).thenReturn(Optional.of(existing));
        when(settings.save(any(HotelSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updatePersistsAllHotelFieldsAndTimestamp() {
        HotelSettingsRequest request = new HotelSettingsRequest(
                "  Hôtel La Grâce  ", "  Antsirabe  ", " 0340000000 ", "contact@example.com",
                "NIF-123", "STAT-456", "Espèces\nMVola", "Merci de votre visite");

        HotelSettings result = service.update(request);

        assertEquals("Hôtel La Grâce", result.getLegalName());
        assertEquals("Antsirabe", result.getAddress());
        assertEquals("0340000000", result.getPhone());
        assertEquals("contact@example.com", result.getEmail());
        assertEquals("NIF-123", result.getNif());
        assertEquals("STAT-456", result.getStat());
        assertEquals("Espèces\nMVola", result.getPaymentDetails());
        assertEquals("Merci de votre visite", result.getInvoiceFooter());
        assertEquals(OffsetDateTime.now(Clock.fixed(Instant.parse("2026-11-24T05:00:00Z"),
                ZoneId.of("Indian/Antananarivo"))), result.getUpdatedAt());

        ArgumentCaptor<HotelSettings> saved = ArgumentCaptor.forClass(HotelSettings.class);
        verify(settings).save(saved.capture());
        assertEquals(result, saved.getValue());
    }

    @Test
    void updateStoresBlankOptionalFieldsAsNull() {
        HotelSettingsRequest request = new HotelSettingsRequest("Hôtel", " ", null, "", null, null, " ", null);

        HotelSettings result = service.update(request);

        assertEquals(null, result.getAddress());
        assertEquals(null, result.getPhone());
        assertEquals(null, result.getEmail());
        assertEquals(null, result.getPaymentDetails());
    }
}
