// src/main/java/com/esprit/helma_backend/dto/TransactionDto.java
package com.esprit.helma_backend.dto;

import com.esprit.helma_backend.entities.Transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public sealed interface TransactionDto permits
        TransactionDto.Create,
        TransactionDto.Update,
        TransactionDto.Response {

    record Create(
            @NotNull Long userId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String category,
            @NotNull TransactionType type,
            Instant txnDate
    ) implements TransactionDto {}

    record Update(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String category,
            @NotNull TransactionType type,
            Instant txnDate
    ) implements TransactionDto {}

    record Response(
            Long id,
            Long userId,
            BigDecimal amount,
            String category,
            TransactionType type,
            Instant txnDate
    ) implements TransactionDto {}
}