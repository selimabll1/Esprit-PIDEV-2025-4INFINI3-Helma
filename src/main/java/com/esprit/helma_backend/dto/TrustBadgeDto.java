package com.esprit.helma_backend.dto;

import com.esprit.helma_backend.entities.BadgeLevel;

import java.math.BigDecimal;
import java.time.Instant;

public class TrustBadgeDto {

    public record Response(
            Long userId,
            BadgeLevel level,
            BigDecimal creditCapacity,
            String reasons,
            Instant computedAt
    ) {}
}