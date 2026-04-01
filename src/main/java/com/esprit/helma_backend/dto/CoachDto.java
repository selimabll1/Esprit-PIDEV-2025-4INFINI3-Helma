package com.esprit.helma_backend.dto;

import java.time.Instant;

public class CoachDto {

    public record SendRequest(
            Long userId,
            String message
    ) {}

    public record Response(
            Long messageId,
            Long sessionId,
            String content,
            Instant createdAt
    ) {}

    public record MessageResponse(
            Long messageId,
            String role,
            String content,
            Instant createdAt
    ) {}
}