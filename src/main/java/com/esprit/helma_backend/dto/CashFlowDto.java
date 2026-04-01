package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public sealed interface CashFlowDto permits CashFlowDto.Response {

    record Response(
            Long id,
            Long userId,
            LocalDate monthStart,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netFlow,
            BigDecimal cumulativeBalance,
            Instant updatedAt
    ) implements CashFlowDto {}
}