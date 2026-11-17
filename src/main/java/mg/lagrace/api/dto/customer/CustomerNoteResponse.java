package mg.lagrace.api.dto.customer;

import java.time.OffsetDateTime;

public record CustomerNoteResponse(Long id, String noteType, String content, boolean isPinned,
        boolean isAlert, Integer version, String createdBy, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, OffsetDateTime archivedAt) {}