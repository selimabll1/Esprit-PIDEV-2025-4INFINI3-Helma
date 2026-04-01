package com.esprit.helma_backend.dto;

import java.time.Instant;
import java.util.List;

public class AdminRiskCaseDto {

    public record CaseResponse(
            Long id,
            Long userId,
            String userFullName,
            String userEmail,
            Boolean userIsEntrepreneur,
            Integer riskLevel,
            RiskSeverity severity,
            Long assignedAdminId,
            String assignedAdminName,
            String status,
            Instant detectedAt,
            String riskReasons
    ) {}

    public record StatsResponse(
            long totalOpen,
            long totalResolved,
            long highRisk,
            long mediumRisk,
            long lowRisk,
            long unassigned
    ) {}

    public record DashboardResponse(
            StatsResponse stats,
            List<CaseResponse> cases
    ) {}

    public record ResolveRequest(String resolutionNote) {}

    public record AssignRequest(Long adminId) {}

    public record AdminOption(
            Long id,
            String fullName,
            String email
    ) {}

    public enum RiskSeverity {
        LOW, MEDIUM, HIGH, CRITICAL;

        public static RiskSeverity of(int level) {
            if (level >= 90) return CRITICAL;
            if (level >= 75) return HIGH;
            if (level >= 50) return MEDIUM;
            return LOW;
        }
    }
}