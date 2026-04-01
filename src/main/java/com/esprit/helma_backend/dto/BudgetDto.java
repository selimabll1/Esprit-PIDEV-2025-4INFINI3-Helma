package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BudgetDto {

    public record Create(
            Long userId,
            BigDecimal limitAmount,
            LocalDate monthStart,
            String category
    ) {}

    public record Update(
            BigDecimal limitAmount,
            LocalDate monthStart,
            String category
    ) {}

    public record Response(
            Long id,
            Long userId,
            BigDecimal limitAmount,
            LocalDate monthStart,
            String category
    ) {}

    public record TrustBudgetResponse(
            Long userId,
            boolean entrepreneur,
            String baseMethod,
            BigDecimal base,
            BigDecimal riskScore,
            BigDecimal budgetUsage,
            int maxOpenRisk,
            BigDecimal trustBudget,
            String badgeLevel,
            List<String> badgeReasons,
            List<String> reasons
    ) {}
}