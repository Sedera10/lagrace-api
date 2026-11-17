package mg.lagrace.api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerDuplicateResponse(List<Candidate> candidates) {
    public record Candidate(Long id, String fullName, String phone, long bookingsCount,
            LocalDate lastVisit, BigDecimal balanceDue, List<String> reasons) {}
}