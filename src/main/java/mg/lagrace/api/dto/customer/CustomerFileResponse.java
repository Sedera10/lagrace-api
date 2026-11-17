package mg.lagrace.api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Read model for the customer profile. Amounts originate from v_customer_summary. */
public record CustomerFileResponse(Identity identity, Summary summary, String level,
        List<String> levelReasons, List<Tag> tags, List<PinnedNote> pinnedNotes) {
    public record Identity(Long id, Integer version, String lastname, String firstname,
            LocalDate dateOfBirth, String phone, String email, String address, String nationality,
            Long nationalityId, String documentTypeLabel, Long documentId, String documentNumber, String preferredLanguage,
            boolean contactConsent, boolean isActive, boolean canModify, boolean sensitiveDataReadable) {}
    public record Summary(Long idCustomer, long bookingsCount, long canceledCount,
            LocalDate firstVisit, LocalDate lastVisit, LocalDate nextVisit,
            BigDecimal totalBilled, BigDecimal totalPaid, BigDecimal balanceDue,
            BigDecimal refundDue, long stayedNights, List<String> servicesUsed, long completedCount) {}
    public record Tag(Long id, String name, String colorCode) {}
    public record PinnedNote(Long id, String noteType, String content, boolean isAlert, String createdAt) {}
}
