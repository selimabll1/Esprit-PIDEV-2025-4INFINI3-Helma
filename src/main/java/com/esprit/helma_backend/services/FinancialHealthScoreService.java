package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.*;
import com.esprit.helma_backend.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FinancialHealthScoreService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BurnRateService burnRateService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;
    private final RiskCaseService riskCaseService;
    private final CashFlowService cashFlowService;
    private final TransactionRepository transactionRepository;

    public FinancialHealthScoreService(BurnRateService burnRateService,
                                       BudgetService budgetService,
                                       SavingsGoalService savingsGoalService,
                                       RiskCaseService riskCaseService,
                                       CashFlowService cashFlowService,
                                       TransactionRepository transactionRepository) {
        this.burnRateService = burnRateService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
        this.riskCaseService = riskCaseService;
        this.cashFlowService = cashFlowService;
        this.transactionRepository = transactionRepository;
    }

    public HealthScoreDto.Response compute(Long userId) {
        // Important fix:
        // if the user has no transaction history at all, return NO DATA instead of a fake neutral score
        if (!hasAnyTransactionHistory(userId)) {
            return noDataResponse(userId);
        }

        BurnRateDto.Response burn = burnRateService.compute(userId);
        BigDecimal runwayScore = computeRunwayScore(burn.runwayMonths());

        LocalDate monthStart = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);
        BigDecimal budgetScore = computeBudgetScore(userId, monthStart);
        BigDecimal savingsScore = computeSavingsScore(userId);
        BigDecimal stabilityScore = computeStabilityScore(userId);
        BigDecimal riskScore = computeRiskScore(userId);

        BigDecimal score = runwayScore.multiply(new BigDecimal("0.30"))
                .add(budgetScore.multiply(new BigDecimal("0.25")))
                .add(savingsScore.multiply(new BigDecimal("0.20")))
                .add(stabilityScore.multiply(new BigDecimal("0.15")))
                .add(riskScore.multiply(new BigDecimal("0.10")))
                .setScale(2, RoundingMode.HALF_UP);

        List<String> highlights = new ArrayList<>();
        highlights.add("runway=" + burn.runwayMonths());
        highlights.add("budgetScore=" + budgetScore);
        highlights.add("savingsScore=" + savingsScore);
        highlights.add("stabilityScore=" + stabilityScore);
        highlights.add("riskScore=" + riskScore);

        if (burn.runwayMonths() == null) {
            highlights.add("Runway encore peu interprétable: historique insuffisant.");
        } else if (burn.runwayMonths().compareTo(new BigDecimal("2")) < 0) {
            highlights.add("La trésorerie projetée est tendue.");
        } else if (burn.runwayMonths().compareTo(new BigDecimal("4")) >= 0) {
            highlights.add("La trésorerie actuelle donne un bon coussin.");
        }

        BigDecimal finalScore = clamp(score);

        return new HealthScoreDto.Response(
                userId,
                finalScore,
                labelOf(finalScore),
                runwayScore,
                budgetScore,
                savingsScore,
                stabilityScore,
                riskScore,
                highlights,
                LocalDate.now()
        );
    }

    private boolean hasAnyTransactionHistory(Long userId) {
        Instant firstTxnDate = transactionRepository.findMinTxnDateByUserId(userId);
        return firstTxnDate != null;
    }

    private HealthScoreDto.Response noDataResponse(Long userId) {
        return new HealthScoreDto.Response(
                userId,
                null,
                "-",
                null,
                null,
                null,
                null,
                null,
                List.of("Pas assez de données pour calculer un Health Score fiable."),
                LocalDate.now()
        );
    }

    private BigDecimal computeRunwayScore(BigDecimal runwayMonths) {
        if (runwayMonths == null) return new BigDecimal("45");
        if (runwayMonths.compareTo(ZERO) <= 0) return new BigDecimal("0");
        if (runwayMonths.compareTo(new BigDecimal("1")) < 0) return new BigDecimal("15");
        if (runwayMonths.compareTo(new BigDecimal("2")) < 0) return new BigDecimal("35");
        if (runwayMonths.compareTo(new BigDecimal("4")) < 0) return new BigDecimal("70");
        if (runwayMonths.compareTo(new BigDecimal("6")) < 0) return new BigDecimal("85");
        return new BigDecimal("100");
    }

    private BigDecimal computeBudgetScore(Long userId, LocalDate monthStart) {
        List<BudgetDto.Response> budgets = budgetService.getByUser(userId).stream()
                .filter(b -> monthStart.equals(b.monthStart()))
                .toList();

        if (budgets.isEmpty()) return new BigDecimal("50");

        BigDecimal totalBudget = budgets.stream()
                .map(BudgetDto.Response::limitAmount)
                .map(this::nz)
                .reduce(ZERO, BigDecimal::add);

        if (totalBudget.compareTo(ZERO) <= 0) return new BigDecimal("50");

        BigDecimal spent = estimateSpentForMonth(userId, monthStart);
        BigDecimal usage = spent.divide(totalBudget, 4, RoundingMode.HALF_UP);

        if (usage.compareTo(new BigDecimal("0.80")) <= 0) return new BigDecimal("100");
        if (usage.compareTo(new BigDecimal("1.00")) <= 0) return new BigDecimal("85");
        if (usage.compareTo(new BigDecimal("1.10")) <= 0) return new BigDecimal("70");
        if (usage.compareTo(new BigDecimal("1.25")) <= 0) return new BigDecimal("50");
        return new BigDecimal("20");
    }

    private BigDecimal estimateSpentForMonth(Long userId, LocalDate monthStart) {
        ZoneId zone = ZoneId.systemDefault();
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        BigDecimal spent = transactionRepository.sumExpenseForUserBetween(userId, from, to);
        return nz(spent);
    }

    private BigDecimal computeSavingsScore(Long userId) {
        List<SavingsGoalDto.Response> goals = savingsGoalService.getByUser(userId);
        if (goals.isEmpty()) return new BigDecimal("50");

        BigDecimal sum = ZERO;
        int count = 0;

        for (SavingsGoalDto.Response goal : goals) {
            BigDecimal target = nz(goal.targetAmount());
            if (target.compareTo(ZERO) <= 0) continue;

            BigDecimal progress = nz(goal.currentAmount())
                    .divide(target, 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);

            sum = sum.add(progress.min(HUNDRED));
            count++;
        }

        if (count == 0) return new BigDecimal("50");
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeStabilityScore(Long userId) {
        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId);
        if (history.size() < 2) return new BigDecimal("50");

        int start = Math.max(0, history.size() - 3);
        List<CashFlowDto.Response> sample = history.subList(start, history.size());

        BigDecimal avgIncome = sample.stream()
                .map(CashFlowDto.Response::totalIncome)
                .map(this::nz)
                .reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sample.size()), 4, RoundingMode.HALF_UP);

        if (avgIncome.compareTo(ZERO) <= 0) return new BigDecimal("30");

        BigDecimal avgAbsDeviation = sample.stream()
                .map(CashFlowDto.Response::totalIncome)
                .map(this::nz)
                .map(v -> v.subtract(avgIncome).abs())
                .reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sample.size()), 4, RoundingMode.HALF_UP);

        BigDecimal variationRatio = avgAbsDeviation.divide(avgIncome, 4, RoundingMode.HALF_UP);
        BigDecimal score = HUNDRED.subtract(variationRatio.multiply(HUNDRED));

        return clamp(score.max(new BigDecimal("10")));
    }

    private BigDecimal computeRiskScore(Long userId) {
        long open = riskCaseService.getByUser(userId).stream()
                .filter(r -> "OPEN".equalsIgnoreCase(r.status()))
                .count();

        if (open == 0) return new BigDecimal("100");
        if (open == 1) return new BigDecimal("70");
        if (open == 2) return new BigDecimal("40");
        return new BigDecimal("20");
    }

    private String labelOf(BigDecimal score) {
        if (score == null) return "-";
        if (score.compareTo(new BigDecimal("85")) >= 0) return "EXCELLENT";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "SOLIDE";
        if (score.compareTo(new BigDecimal("50")) >= 0) return "STABLE";
        return "FRAGILE";
    }

    private BigDecimal clamp(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(ZERO) < 0) return ZERO;
        if (score.compareTo(HUNDRED) > 0) return HUNDRED;
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}