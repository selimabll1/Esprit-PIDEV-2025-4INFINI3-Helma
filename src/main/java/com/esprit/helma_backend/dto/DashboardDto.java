package com.esprit.helma_backend.dto;

import java.util.List;

public class DashboardDto {

    public record Response(
            Long userId,
            CashFlowDto.Response currentCashFlow,
            BurnRateDto.Response burnRate,
            ForecastDto.Response forecast,
            HealthScoreDto.Response healthScore,
            TrustBadgeDto.Response trustBadge,
            IncomeStatementDto.Response incomeStatement,
            List<BudgetDto.Response> currentBudgets,
            List<SavingsGoalDto.Response> savingsGoals,
            List<RiskCaseDto.Response> openRiskCases,
            String mainAlert,
            String mainPositive,
            String nextBestAction
    ) {}
}