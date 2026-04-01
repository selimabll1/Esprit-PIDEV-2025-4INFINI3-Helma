package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class IncomeStatementDto {

    public record Response(
            Long id,
            Long userId,
            LocalDate monthStart,
            BigDecimal totalRevenue,
            BigDecimal totalExpenses,
            BigDecimal netResult,
            BigDecimal marginRate,
            Instant updatedAt
    ) {}
}