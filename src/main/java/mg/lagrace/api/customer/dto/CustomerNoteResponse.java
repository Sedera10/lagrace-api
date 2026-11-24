package mg.lagrace.api.customer.dto;

import mg.lagrace.api.customer.Customer;

import java.time.OffsetDateTime;

public record CustomerNoteResponse(Long id, String noteType, String content, boolean isPinned,
        boolean isAlert, Integer version, String createdBy, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, OffsetDateTime archivedAt) {}