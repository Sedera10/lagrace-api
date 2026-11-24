package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerSearchResponse(Long id, String fullName, String phone,
        List<CustomerListResponse.Tag> tags, BigDecimal balanceDue, LocalDate nextVisit,
        boolean hasAlert) {}