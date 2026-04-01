package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class HealthScoreDto {

    public record Response(
            Long userId,
            BigDecimal score,
            String label,
            BigDecimal runwayScore,
            BigDecimal budgetScore,
            BigDecimal savingsScore,
            BigDecimal stabilityScore,
            BigDecimal riskScore,
            List<String> highlights,
            LocalDate computedAt
    ) {}
}