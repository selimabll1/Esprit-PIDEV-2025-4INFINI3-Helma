package com.esprit.helma_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class RiskCaseDto {

    public record Create(
            @NotNull Long userId,
            @NotNull @Min(0) @Max(100) Integer riskLevel,
            Long assignedAdminId,
            String status   // optional: "OPEN" or "RESOLVED"
    ) {}

    public record Update(
            @NotNull @Min(0) @Max(100) Integer riskLevel,
            Long assignedAdminId,
            String status   // optional
    ) {}

    public record Response(
            Long id,
            Long userId,
            Integer riskLevel,
            Long assignedAdminId,
            String status,
            Instant detectedAt
    ) {}
}