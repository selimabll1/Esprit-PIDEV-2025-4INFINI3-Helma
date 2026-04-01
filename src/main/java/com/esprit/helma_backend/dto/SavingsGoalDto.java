package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class SavingsGoalDto {

    public record Create(
            Long userId,
            String name,
            BigDecimal targetAmount,
            LocalDate deadline
    ) {}

    public record AddProgress(
            BigDecimal amount
    ) {}

    public record Response(
            Long id,
            Long userId,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate deadline,
            BigDecimal weeklyTarget,
            Boolean completed,
            Instant createdAt
    ) {}
}