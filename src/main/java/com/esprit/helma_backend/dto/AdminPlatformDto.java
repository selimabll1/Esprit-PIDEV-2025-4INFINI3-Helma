package com.esprit.helma_backend.dto;

import com.esprit.helma_backend.entities.User;
import jakarta.validation.constraints.NotNull;

public class AdminPlatformDto {

    public record PlatformStats(
            long totalUsers,
            long totalEntrepreneurs,
            long totalBasicUsers,
            long totalAdmins,
            long openRiskCases,
            long resolvedRiskCases,
            long totalTransactions
    ) {}

    public record UserRow(
            Long id,
            String fullName,
            String email,
            User.Role role,
            Boolean isEntrepreneur
    ) {}

    public record RoleChangeRequest(
            @NotNull User.Role role
    ) {}
}
