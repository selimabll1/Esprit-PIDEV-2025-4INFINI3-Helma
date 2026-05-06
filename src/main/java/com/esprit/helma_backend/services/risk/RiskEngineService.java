package com.esprit.helma_backend.services.risk;

import com.esprit.helma_backend.entities.Budget;
import com.esprit.helma_backend.entities.Transaction;
import com.esprit.helma_backend.entities.Transaction.TransactionType;
import com.esprit.helma_backend.repositories.BudgetRepository;
import com.esprit.helma_backend.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEngineService {

    private final BudgetRepository budgetRepo;
    private final TransactionRepository txRepo;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_POINT_FIVE = new BigDecimal("1.5");
    private static final BigDecimal TWO = new BigDecimal("2.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final BigDecimal W_USAGE = new BigDecimal("0.40");
    private static final BigDecimal W_SPIKE = new BigDecimal("0.25");
    private static final BigDecimal W_BIGTX = new BigDecimal("0.35");

    private static final BigDecimal BIGTX_MIN = new BigDecimal("200");
    private static final BigDecimal BIGTX_MAX = new BigDecimal("2000");
    private static final int TRIGGER_AT = 50;
    private static final BigDecimal HIGH_SINGLE_MULTIPLIER = new BigDecimal("5");

    public RiskEngineService(BudgetRepository budgetRepo, TransactionRepository txRepo) {
        this.budgetRepo = budgetRepo;
        this.txRepo = txRepo;
    }

    @Transactional(readOnly = true)
    public RiskDecision evaluate(Transaction savedTx) {

        if (savedTx.getType() != TransactionType.EXPENSE) {
            return new RiskDecision(false, 0, List.of("INCOME_TRANSACTION_SKIPPED"));
        }

        Long userId = savedTx.getUser().getId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = currentMonthStart.plusMonths(1);

        Instant currentMonthStartI = currentMonthStart.atStartOfDay(zone).toInstant();
        Instant nextMonthStartI = nextMonthStart.atStartOfDay(zone).toInstant();

        List<Budget> currentBudgets =
                budgetRepo.findByUserIdAndMonthStartOrderByCategoryAsc(userId, currentMonthStart);

        BigDecimal budgetLimit = currentBudgets.stream()
                .map(Budget::getLimitAmount)
                .map(this::nz)
                .reduce(ZERO, BigDecimal::add);

        if (budgetLimit.compareTo(ZERO) <= 0) {
            budgetLimit = null;
        }

        BigDecimal spendCurrentMonth =
                nz(txRepo.sumExpenseForUserBetween(userId, currentMonthStartI, nextMonthStartI));

        LocalDate m1Start = currentMonthStart.minusMonths(1);
        LocalDate m2Start = currentMonthStart.minusMonths(2);
        LocalDate m3Start = currentMonthStart.minusMonths(3);

        Instant m1StartI = m1Start.atStartOfDay(zone).toInstant();
        Instant m2StartI = m2Start.atStartOfDay(zone).toInstant();
        Instant m3StartI = m3Start.atStartOfDay(zone).toInstant();

        long distinctMonths = txRepo.countDistinctYearMonthForUserBetween(userId, m3StartI, currentMonthStartI);

        BigDecimal base;
        String baseMethod;

        if (distinctMonths >= 3) {
            BigDecimal s3 = nz(txRepo.sumExpenseForUserBetween(userId, m3StartI, m2StartI));
            BigDecimal s2 = nz(txRepo.sumExpenseForUserBetween(userId, m2StartI, m1StartI));
            BigDecimal s1 = nz(txRepo.sumExpenseForUserBetween(userId, m1StartI, currentMonthStartI));
            base = s1.add(s2).add(s3).divide(new BigDecimal("3"), 6, RoundingMode.HALF_UP);
            baseMethod = "3M_AVG";
        } else if (budgetLimit != null) {
            base = nz(budgetLimit);
            baseMethod = "BUDGET";
        } else {
            base = ZERO;
            baseMethod = "NONE";
        }

        List<String> reasons = new ArrayList<>();

        BigDecimal usageScore = ZERO;
        if (budgetLimit != null && budgetLimit.compareTo(ZERO) > 0) {
            BigDecimal budgetUsage = spendCurrentMonth.divide(budgetLimit, 6, RoundingMode.HALF_UP);
            BigDecimal capped = min(budgetUsage, ONE_POINT_FIVE);
            usageScore = capped.divide(ONE_POINT_FIVE, 6, RoundingMode.HALF_UP).multiply(HUNDRED);

            if (budgetUsage.compareTo(BigDecimal.ONE) > 0) {
                reasons.add("OVER_BUDGET");
            } else if (budgetUsage.compareTo(new BigDecimal("0.90")) >= 0) {
                reasons.add("NEAR_BUDGET_LIMIT");
            }
        } else {
            reasons.add("NO_BUDGET_SET");
        }

        BigDecimal spikeScore = ZERO;
        if (base.compareTo(ZERO) > 0) {
            BigDecimal spikeRatio = spendCurrentMonth.divide(base, 6, RoundingMode.HALF_UP);
            BigDecimal capped = min(spikeRatio, TWO);
            spikeScore = capped.divide(TWO, 6, RoundingMode.HALF_UP).multiply(HUNDRED); //Même si l’utilisateur n’a pas dépassé officiellement son budget, une hausse brutale par rapport à son comportement normal peut être un signal faible.

            if (spikeRatio.compareTo(new BigDecimal("1.20")) >= 0) {
                reasons.add("SPENDING_SPIKE_VS_BASELINE");
            }
        } else {
            reasons.add("NO_BASELINE_AVAILABLE_" + baseMethod);
        }

        Instant now = Instant.now();
        Instant last7d = now.minus(Duration.ofDays(7));
        BigDecimal max7d = nz(txRepo.maxExpenseForUserBetween(userId, last7d, now));//e moteur regarde la plus grande dépense sur les 7 derniers jours ://
//transformation linéaire entre 200 et 2000 vers un score 0..100
//si max7d >= 1000, raison LARGE_TXN_LAST_7D
        BigDecimal highAmountScore = mapLinearTo100(max7d, BIGTX_MIN, BIGTX_MAX);
        if (max7d.compareTo(new BigDecimal("1000")) >= 0) {
            reasons.add("LARGE_TXN_LAST_7D");
        }

        // HIGH_SINGLE_AMOUNT: single expense > 5x average monthly baseline always triggers
        BigDecimal txAmount = nz(savedTx.getAmount());
        if (base.compareTo(ZERO) > 0) {
            BigDecimal multiplier = txAmount.divide(base, 6, RoundingMode.HALF_UP);
            if (multiplier.compareTo(HIGH_SINGLE_MULTIPLIER) >= 0) {
                reasons.add("HIGH_SINGLE_AMOUNT");
                highAmountScore = HUNDRED;
            }
        }

        BigDecimal risk = W_USAGE.multiply(usageScore)
                .add(W_SPIKE.multiply(spikeScore))
                .add(W_BIGTX.multiply(highAmountScore));

        int riskLevel = clampInt(risk.setScale(0, RoundingMode.HALF_UP).intValue(), 0, 100);
        boolean triggered = riskLevel >= TRIGGER_AT || max7d.compareTo(new BigDecimal("2000")) >= 0;

        if (reasons.contains("HIGH_SINGLE_AMOUNT") && riskLevel < 70) {
            riskLevel = 70;
            triggered = true;
        }

        if (triggered) reasons.add("RISK_TRIGGERED");
        reasons.add("BASE_METHOD_" + baseMethod);

        return new RiskDecision(triggered, riskLevel, reasons);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static BigDecimal mapLinearTo100(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return ZERO;
        if (value.compareTo(min) <= 0) return ZERO;
        if (value.compareTo(max) >= 0) return HUNDRED;
        return value.subtract(min)
                .divide(max.subtract(min), 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}