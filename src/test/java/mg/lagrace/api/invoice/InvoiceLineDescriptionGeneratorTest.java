package mg.lagrace.api.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceLineDescriptionGeneratorTest {
 @Test void formats_room_with_singular_night() {
  assertEquals("Chambre double · Chambre 207 · 26/10 → 27/10 (1 nuit)", InvoiceLineDescriptionGenerator.room("double", "207", LocalDate.of(2026,10,26), LocalDate.of(2026,10,27), 1, false));
 }
 @Test void marks_included_items_as_offered() {
  assertEquals("Salle de réunion Baobab · Journée · 26/10 (offert)", InvoiceLineDescriptionGenerator.meetingRoom("Baobab", "Journée", LocalDate.of(2026,10,26), true));
 }
 @Test void formats_other_without_adding_a_service_prefix() { assertEquals("Remise fidélité", InvoiceLineDescriptionGenerator.other("Remise fidélité")); }
}
