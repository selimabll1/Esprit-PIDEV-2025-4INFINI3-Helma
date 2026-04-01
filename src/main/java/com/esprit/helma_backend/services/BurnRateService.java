package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.BurnRateDto;
import com.esprit.helma_backend.dto.BurnRateDto.MonthExpense;
import com.esprit.helma_backend.dto.BurnRateDto.RunwayStatus;
import com.esprit.helma_backend.entities.CashFlowSummary;
import com.esprit.helma_backend.repositories.CashFlowSummaryRepository;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BurnRateService {

    private static final BigDecimal RUNWAY_CRITICAL = new BigDecimal("2");
    private static final BigDecimal RUNWAY_WARNING = new BigDecimal("4");
    private static final int RISK_LEVEL_LOW_RUNWAY = 75;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TransactionRepository txRepo;
    private final CashFlowSummaryRepository cashFlowRepo;
    private final UserRepository userRepo;
    private final RiskCaseService riskCaseService;

    public BurnRateService(TransactionRepository txRepo,
                           CashFlowSummaryRepository cashFlowRepo,
                           UserRepository userRepo,
                           RiskCaseService riskCaseService) {
        this.txRepo = txRepo;
        this.cashFlowRepo = cashFlowRepo;
        this.userRepo = userRepo;
        this.riskCaseService = riskCaseService;
    }

    public BurnRateDto.Response compute(Long userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate effectiveCurrentMonthStart = resolveEffectiveCurrentMonthStart(userId, zone);

        List<MonthExpense> breakdown = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            LocalDate mStart = effectiveCurrentMonthStart.minusMonths(i);
            LocalDate mEnd = mStart.plusMonths(1);

            Instant from = mStart.atStartOfDay(zone).toInstant();
            Instant to = mEnd.atStartOfDay(zone).toInstant();

            BigDecimal expense = nz(txRepo.sumExpenseForUserBetween(userId, from, to));

            if (expense.compareTo(ZERO) > 0) {
                breakdown.add(0, new MonthExpense(mStart, expense));
            }
        }

        int monthsUsed = breakdown.size();
        BigDecimal burnRate;
        String burnRateMethod;

        if (monthsUsed == 0) {
            burnRate = ZERO;
            burnRateMethod = "NO_DATA";
        } else {
            BigDecimal sum = breakdown.stream()
                    .map(MonthExpense::totalExpense)
                    .reduce(ZERO, BigDecimal::add);

            burnRate = sum.divide(BigDecimal.valueOf(monthsUsed), 2, RoundingMode.HALF_UP);
            burnRateMethod = switch (monthsUsed) {
                case 1 -> "1M_AVG";
                case 2 -> "2M_AVG";
                default -> "3M_AVG";
            };
        }

        BigDecimal currentBalance = latestCumulativeBalance(userId, effectiveCurrentMonthStart);

        BigDecimal runwayMonths = null;
        if (burnRate.compareTo(ZERO) > 0 && currentBalance.compareTo(ZERO) > 0) {
            runwayMonths = currentBalance.divide(burnRate, 2, RoundingMode.HALF_UP);
        }

        RunwayStatus status = determineStatus(runwayMonths);

        boolean riskTriggered = false;
        if (status == RunwayStatus.CRITICAL) {
            riskCaseService.upsertOpenCase(
                    userId,
                    RISK_LEVEL_LOW_RUNWAY,
                    List.of(
                            "LOW_RUNWAY",
                            "BURN_RATE_METHOD_" + burnRateMethod,
                            "RUNWAY_MONTHS_" + (runwayMonths != null ? runwayMonths.toPlainString() : "NEG")
                    )
            );
            riskTriggered = true;
        }

        List<String> tips = generateFinCoachTips(status, runwayMonths, burnRate, currentBalance);

        LocalDate projectedZeroDate = null;
        if (runwayMonths != null) {
            long daysToZero = runwayMonths.multiply(BigDecimal.valueOf(30)).longValue();
            projectedZeroDate = today.plusDays(daysToZero);
        }

        return new BurnRateDto.Response(
                userId,
                burnRate,
                currentBalance,
                runwayMonths,
                status,
                riskTriggered,
                burnRateMethod,
                monthsUsed,
                breakdown,
                tips,
                today,
                projectedZeroDate
        );
    }

    private LocalDate resolveEffectiveCurrentMonthStart(Long userId, ZoneId zone) {
        LocalDate realCurrentMonthStart = LocalDate.now(zone).withDayOfMonth(1);

        Instant latestTxnInstant = txRepo.findMaxTxnDateByUserId(userId);
        if (latestTxnInstant == null) {
            return realCurrentMonthStart;
        }

        LocalDate latestTxnMonthStart = latestTxnInstant.atZone(zone).toLocalDate().withDayOfMonth(1);
        LocalDate monthAfterLatestTxn = latestTxnMonthStart.plusMonths(1);

        return monthAfterLatestTxn.isBefore(realCurrentMonthStart)
                ? monthAfterLatestTxn
                : realCurrentMonthStart;
    }

    private BigDecimal latestCumulativeBalance(Long userId, LocalDate currentMonthStart) {
        Optional<CashFlowSummary> current =
                cashFlowRepo.findByUserIdAndMonthStart(userId, currentMonthStart);

        if (current.isPresent()) {
            return nz(current.get().getCumulativeBalance());
        }

        Optional<CashFlowSummary> prev =
                cashFlowRepo.findTopByUserIdAndMonthStartLessThanOrderByMonthStartDesc(userId, currentMonthStart);

        return prev.map(s -> nz(s.getCumulativeBalance())).orElse(ZERO);
    }

    private RunwayStatus determineStatus(BigDecimal runwayMonths) {
        if (runwayMonths == null) return RunwayStatus.CRITICAL;
        if (runwayMonths.compareTo(ZERO) < 0) return RunwayStatus.CRITICAL;
        if (runwayMonths.compareTo(RUNWAY_CRITICAL) < 0) return RunwayStatus.CRITICAL;
        if (runwayMonths.compareTo(RUNWAY_WARNING) < 0) return RunwayStatus.WARNING;
        return RunwayStatus.HEALTHY;
    }

    private List<String> generateFinCoachTips(RunwayStatus status,
                                              BigDecimal runwayMonths,
                                              BigDecimal burnRate,
                                              BigDecimal balance) {
        List<String> tips = new ArrayList<>();

        switch (status) {
            case CRITICAL -> {
                if (runwayMonths != null && runwayMonths.compareTo(ZERO) > 0) {
                    tips.add(String.format(
                            "🔴 Tu as %.1f mois de runway — agis immédiatement.",
                            runwayMonths.doubleValue()));
                } else {
                    tips.add("🔴 Ton solde est négatif ou ton burn rate n'est pas calculable. Revois tes dépenses d'urgence.");
                }
                tips.add("Identifie tes 3 plus gros postes de dépenses et réduis-les de 20% ce mois.");
                tips.add("Relance tes clients avec des factures impayées — chaque TND compte.");
                tips.add("Considère un micro-financement ou une avance sur facture pour sécuriser 2 mois de trésorerie.");
            }
            case WARNING -> {
                tips.add(String.format(
                        "🟡 Tu as %.1f mois de runway — surveille tes dépenses de près.",
                        runwayMonths != null ? runwayMonths.doubleValue() : 0));
                tips.add("Évite toute dépense non-essentielle > 200 TND ce mois.");
                tips.add("Cherche à augmenter tes revenus : upsell clients existants, nouvelle prestation.");
                tips.add("Mets en place un budget hebdomadaire pour garder le contrôle.");
            }
            case HEALTHY -> {
                tips.add(String.format(
                        "🟢 Tu as %.1f mois de runway — bonne gestion !",
                        runwayMonths != null ? runwayMonths.doubleValue() : 0));
                tips.add("Continue à maintenir un burn rate stable. Ne laisse pas tes dépenses fixer grow.");
                tips.add("Avec cette trésorerie, c'est le bon moment pour investir dans ta croissance.");
                tips.add("Objectif : 6 mois de runway pour une sécurité optimale.");
            }
        }

        return tips;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}