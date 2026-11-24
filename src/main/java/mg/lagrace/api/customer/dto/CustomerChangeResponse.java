package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerChangeResponse(Long id, UUID operationId, String action, String fieldName,
        String oldValue, String newValue, String reason, String changedBy, OffsetDateTime changedAt) {}