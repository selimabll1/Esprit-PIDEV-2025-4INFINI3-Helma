package com.esprit.helma_backend.dto;

import java.time.Instant;

public class AuditLogDto {

    public record Response(
            Long id,
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String details,
            Instant createdAt
    ) {}
}