package mg.lagrace.api.invoice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Pure formatter used when an invoice line snapshot is created. */
public final class InvoiceLineDescriptionGenerator {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");
    private InvoiceLineDescriptionGenerator() {}

    public static String room(String roomType, String roomRef, LocalDate checkIn, LocalDate checkOut, int nights, boolean included) {
        return offered("Chambre " + roomType + " · Chambre " + roomRef + " · " + date(checkIn) + " → " + date(checkOut)
                + " (" + nights + " " + (nights == 1 ? "nuit" : "nuits") + ")", included);
    }
    public static String pool(String pool, String service, LocalDate date, boolean included) {
        return offered("Piscine " + pool + (blank(service) ? "" : " · " + service) + " · " + date(date), included);
    }
    public static String meetingRoom(String room, String service, LocalDate date, boolean included) {
        return offered("Salle de réunion " + room + (blank(service) ? "" : " · " + service) + " · " + date(date), included);
    }
    public static String space(String space, String eventType, LocalDate date, boolean included) {
        return offered("Espace " + space + (blank(eventType) ? "" : " · " + eventType) + " · " + date(date), included);
    }
    public static String charge(String label, boolean included) { return offered(label, included); }
    public static String other(String label) { return label; }
    private static String offered(String text, boolean included) { return included ? text + " (offert)" : text; }
    private static String date(LocalDate value) { return value.format(DATE); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
