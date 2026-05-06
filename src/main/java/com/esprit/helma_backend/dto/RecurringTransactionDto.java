package com.esprit.helma_backend.dto;

import com.esprit.helma_backend.entities.RecurringTransaction.Frequency;
import com.esprit.helma_backend.entities.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringTransactionDto {

    public record Create(
            @NotNull Long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String category,
            @NotNull TransactionType type,
            @NotNull Frequency frequency,
            String description
    ) {}

    public record Response(
            Long id,
            Long userId,
            BigDecimal amount,
            String category,
            TransactionType type,
            Frequency frequency,
            LocalDate nextDate,
            boolean active,
            String description
    ) {}
}
