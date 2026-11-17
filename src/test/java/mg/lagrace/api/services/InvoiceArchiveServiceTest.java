package mg.lagrace.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mg.lagrace.api.dto.invoice.InvoiceArchiveResponse;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.repositories.InvoiceArchiveRepository;

@ExtendWith(MockitoExtension.class)
class InvoiceArchiveServiceTest {
    @Mock private InvoiceArchiveRepository repository;

    @Test
    void listUsesSameNormalizedFiltersForPageAndStatistics() {
        InvoiceArchiveService service = new InvoiceArchiveService(repository);
        when(repository.count("LAG", "PAID", "ROOM", "INVOICE", null, null)).thenReturn(1L);
        when(repository.findItems("LAG", "PAID", "ROOM", "INVOICE", null, null, 20, 20, null)).thenReturn(List.of());
        InvoiceArchiveResponse.Totals totals = new InvoiceArchiveResponse.Totals(1, 1, BigDecimal.ZERO, new BigDecimal("120.00"));
        when(repository.totals("LAG", "PAID", "ROOM", "INVOICE", null, null)).thenReturn(totals);

        InvoiceArchiveResponse response = service.list(" lag ", "paid", "room", "invoice", null, null, 1, 20, null);

        assertEquals(1, response.totalItems());
        assertEquals(1, response.totalPages());
        assertEquals(totals, response.totals());
        verify(repository).findItems("LAG", "PAID", "ROOM", "INVOICE", null, null, 20, 20, null);
    }

    @Test
    void exportAddsUtf8BomAndUsesFrenchDateAndRawAmounts() {
        InvoiceArchiveService service = new InvoiceArchiveService(repository);
        when(repository.exportItems(null, null, null, null, null, null, null)).thenReturn(List.of(
                new InvoiceArchiveResponse.Item(3L, 8L, "FAC-2026-000003", LocalDate.of(2026, 11, 11),
                        "Hôtel; Test", "Référence", "Chambre", new BigDecimal("1250.50"),
                        new BigDecimal("1250.50"), "Virement", "PAID", "ISSUED", "INVOICE", List.of(), null)));

        String csv = new String(service.exportCsv(null, null, null, null, null, null, null), java.nio.charset.StandardCharsets.UTF_8);

        assertEquals('\uFEFF', csv.charAt(0));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("11/11/2026"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"Hôtel; Test\""));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("1250.50"));
    }

    @Test
    void rejectsSingleCharacterSearch() {
        InvoiceArchiveService service = new InvoiceArchiveService(repository);
        assertThrows(ValidationException.class, () -> service.list("a", null, null, null, null, null, 0, 20, null));
    }
}