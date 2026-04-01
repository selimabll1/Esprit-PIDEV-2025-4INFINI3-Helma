package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ForecastDto {

    public record ForecastMonth(
            LocalDate monthStart,
            BigDecimal predictedIncome,
            BigDecimal predictedExpense,
            BigDecimal predictedNetFlow,
            BigDecimal predictedBalance
    ) {}

    public record Response(
            Long userId,
            int monthsUsed,
            String forecastMethod,
            String confidenceLevel,
            String historyQuality,
            String incomeTrend,
            String expenseTrend,
            BigDecimal incomeSlope,
            BigDecimal expenseSlope,
            BigDecimal avgPredictedIncome,
            BigDecimal avgPredictedExpense,
            BigDecimal avgPredictedNetFlow,
            BigDecimal predictedRunwayMonths,
            LocalDate projectedCashoutDate,
            List<ForecastMonth> months,
            List<String> alerts,
            List<String> explanations,
            LocalDate computedAt
    ) {}
}