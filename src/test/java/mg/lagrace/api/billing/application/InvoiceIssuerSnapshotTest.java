package mg.lagrace.api.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mg.lagrace.api.reference.HotelSettings;
import tools.jackson.databind.ObjectMapper;

class InvoiceIssuerSnapshotTest {
    @Test
    void snapshotPreservesUnicodeAndMultilineSettings() throws Exception {
        HotelSettings settings = new HotelSettings();
        ReflectionTestUtils.setField(settings, "legalName", "Hôtel & Espace La Grâce");
        ReflectionTestUtils.setField(settings, "paymentDetails", "Paiement : MVola — +261 34 00 000 00\nRéférence : numéro de facture");

        var snapshot = new ObjectMapper().readTree(InvoiceIssuanceService.snapshot(settings));

        assertEquals("Hôtel & Espace La Grâce", snapshot.path("legalName").asString());
        assertEquals("Paiement : MVola — +261 34 00 000 00\nRéférence : numéro de facture",
                snapshot.path("paymentDetails").asString());
    }
}
