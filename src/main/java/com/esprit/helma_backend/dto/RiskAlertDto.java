package com.esprit.helma_backend.dto;

import java.time.Instant;
import java.util.List;

public record RiskAlertDto(
        Long userId,
        String riskLevel,       // "HIGH" | "MEDIUM" | "LOW"
        Integer riskScore,      // numeric 0-100
        String message,
        List<String> reasons,
        Instant detectedAt
) {
    public static RiskAlertDto of(Long userId, String message, int riskScore, List<String> reasons) {
        String level = riskScore >= 70 ? "HIGH" : riskScore >= 40 ? "MEDIUM" : "LOW";
        return new RiskAlertDto(userId, level, riskScore, message,
                reasons != null ? reasons : List.of(), Instant.now());
    }
}
