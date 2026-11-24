package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerChangesResponse(List<Operation> items, int page, int size,
        long totalItems, int totalPages) {
    public record Operation(UUID operationId, String action, OffsetDateTime changedAt,
            String changedBy, String reason, List<FieldChange> changes) {}
    public record FieldChange(String fieldName, String oldValue, String newValue) {}
}