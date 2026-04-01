package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.BurnRateDto;
import com.esprit.helma_backend.entities.Budget;
import com.esprit.helma_backend.entities.CashFlowSummary;
import com.esprit.helma_backend.entities.SavingsGoal;
import com.esprit.helma_backend.entities.TrustBadge;
import com.esprit.helma_backend.repositories.BudgetRepository;
import com.esprit.helma_backend.repositories.CashFlowSummaryRepository;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.SavingsGoalRepository;
import com.esprit.helma_backend.repositories.TrustBadgeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class ContextSnapshotBuilder {

    private static final String ND = "Non disponible";
    private static final String YES = "OUI";
    private static final String NO = "NON";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final CashFlowSummaryRepository cashFlowSummaryRepository;
    private final BurnRateService burnRateService;
    private final TrustBadgeRepository trustBadgeRepository;
    private final RiskCaseRepository riskCaseRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BudgetRepository budgetRepository;

    public ContextSnapshotBuilder(CashFlowSummaryRepository cashFlowSummaryRepository,
                                  BurnRateService burnRateService,
                                  TrustBadgeRepository trustBadgeRepository,
                                  RiskCaseRepository riskCaseRepository,
                                  SavingsGoalRepository savingsGoalRepository,
                                  BudgetRepository budgetRepository) {
        this.cashFlowSummaryRepository = cashFlowSummaryRepository;
        this.burnRateService = burnRateService;
        this.trustBadgeRepository = trustBadgeRepository;
        this.riskCaseRepository = riskCaseRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.budgetRepository = budgetRepository;
    }

    public String build(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);

        Optional<CashFlowSummary> currentSummary =
                cashFlowSummaryRepository.findByUserIdAndMonthStart(userId, currentMonthStart);

        CashFlowSummary summary = currentSummary.orElseGet(() ->
                cashFlowSummaryRepository.findTopByUserIdOrderByMonthStartDesc(userId).orElse(null)
        );

        long monthsOfHistory = getMonthsOfHistory(userId);

        BurnRateDto.Response burnRate = safeBurnRate(userId);
        TrustBadge badge = trustBadgeRepository.findByUserId(userId).orElse(null);
        long openRiskCases = riskCaseRepository.countOpenByUserId(userId);
        List<SavingsGoal> activeGoals =
                savingsGoalRepository.findByUserIdAndCompletedFalseOrderByDeadlineAsc(userId);

        List<Budget> currentBudgets =
                budgetRepository.findByUserIdAndMonthStartOrderByCategoryAsc(userId, currentMonthStart);

        BigDecimal totalBudgetLimit = currentBudgets.stream()
                .map(Budget::getLimitAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalBudgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
            totalBudgetLimit = null;
        }

        BigDecimal totalIncome = summary != null ? summary.getTotalIncome() : null;
        BigDecimal totalExpense = summary != null ? summary.getTotalExpense() : null;
        BigDecimal netFlow = summary != null ? summary.getNetFlow() : null;
        BigDecimal cumulativeBalance = summary != null ? summary.getCumulativeBalance() : null;
        LocalDate summaryMonth = summary != null ? summary.getMonthStart() : currentMonthStart;

        BigDecimal burnRateAmount = burnRate != null ? burnRate.burnRate() : null;
        BigDecimal runwayMonths = burnRate != null ? burnRate.runwayMonths() : null;
        String burnRateStatus = burnRate != null && burnRate.status() != null ? burnRate.status().name() : null;
        LocalDate projectedZeroDate = burnRate != null ? burnRate.projectedZeroDate() : null;

        String badgeLevel = badge != null && badge.getLevel() != null ? badge.getLevel().name() : null;
        BigDecimal creditCapacity = badge != null ? badge.getCreditCapacity() : null;

        BigDecimal budgetUsagePercent = calculateBudgetUsage(totalExpense, totalBudgetLimit);

        boolean hasEnoughHistoryForTrend = monthsOfHistory >= 3;
        boolean burnRateUsable = isBurnRateUsable(burnRateAmount, burnRateStatus, monthsOfHistory);
        boolean runwayUsable = burnRateUsable && runwayMonths != null;
        boolean shortTermPositive = netFlow != null && netFlow.compareTo(BigDecimal.ZERO) > 0;
        boolean budgetExists = totalBudgetLimit != null && totalBudgetLimit.compareTo(BigDecimal.ZERO) > 0;
        boolean overBudget = budgetUsagePercent != null && budgetUsagePercent.compareTo(new BigDecimal("100")) > 0;
        boolean budgetAlmostFull = budgetUsagePercent != null
                && budgetUsagePercent.compareTo(new BigDecimal("80")) >= 0
                && budgetUsagePercent.compareTo(new BigDecimal("100")) <= 0;

        String mainAlert = buildMainAlert(openRiskCases, runwayUsable, runwayMonths, overBudget, burnRateUsable, hasEnoughHistoryForTrend);
        String mainPositive = buildMainPositive(shortTermPositive, budgetExists, budgetUsagePercent, badgeLevel, activeGoals);

        StringBuilder sb = new StringBuilder();

        sb.append("=== CONTEXTE FINANCIER STRUCTURÉ HELMA ===\n");
        sb.append("Date d'analyse : ").append(formatDate(today)).append("\n");
        sb.append("Utilisateur : ").append(userId).append("\n\n");

        sb.append("1) FIABILITÉ DU CONTEXTE\n");
        sb.append("- Mois d'historique disponibles : ").append(monthsOfHistory).append("\n");
        sb.append("- Analyse de tendance exploitable : ").append(hasEnoughHistoryForTrend ? YES : NO).append("\n");
        sb.append("- Burn rate interprétable : ").append(burnRateUsable ? YES : NO).append("\n");
        sb.append("- Runway interprétable : ").append(runwayUsable ? YES : NO).append("\n");
        sb.append("- Lecture long terme fiable : ").append((hasEnoughHistoryForTrend && runwayUsable) ? YES : NO).append("\n\n");

        sb.append("2) PHOTO DU MOIS COURANT OU DERNIER MOIS DISPONIBLE\n");
        sb.append("- Mois analysé : ").append(formatMonth(summaryMonth)).append("\n");
        sb.append("- Revenus : ").append(formatMoney(totalIncome)).append(" TND\n");
        sb.append("- Dépenses : ").append(formatMoney(totalExpense)).append(" TND\n");
        sb.append("- Flux net : ").append(formatMoney(netFlow)).append(" TND\n");
        sb.append("- Solde cumulé : ").append(formatMoney(cumulativeBalance)).append(" TND\n");
        sb.append("- Mois globalement positif : ").append(shortTermPositive ? YES : NO).append("\n\n");

        sb.append("3) SANTÉ FINANCIÈRE CALCULÉE\n");
        sb.append("- Burn rate mensuel : ").append(formatMoney(burnRateAmount)).append(" TND/mois\n");
        sb.append("- Statut burn rate : ").append(valueOrNd(burnRateStatus)).append("\n");
        sb.append("- Runway : ").append(formatDecimal(runwayMonths)).append(" mois\n");
        sb.append("- Date de rupture estimée : ")
                .append(projectedZeroDate != null ? formatDate(projectedZeroDate) : "Non calculable")
                .append("\n\n");

        sb.append("4) PROFIL DE RISQUE ET CONFIANCE\n");
        sb.append("- Trust Badge : ").append(valueOrNd(badgeLevel)).append("\n");
        sb.append("- Capacité d'emprunt : ").append(formatMoney(creditCapacity)).append(" TND\n");
        sb.append("- Cas de risque ouverts : ").append(openRiskCases).append("\n\n");

        sb.append("5) BUDGET DU MOIS\n");
        sb.append("- Nombre de budgets du mois : ").append(currentBudgets.size()).append("\n");
        sb.append("- Budget défini : ").append(budgetExists ? YES : NO).append("\n");
        sb.append("- Plafond total fixé : ").append(formatMoney(totalBudgetLimit)).append(" TND\n");
        sb.append("- Dépenses actuelles : ").append(formatMoney(totalExpense)).append(" TND\n");
        sb.append("- Utilisation du budget : ").append(formatPercent(budgetUsagePercent)).append("\n");
        sb.append("- Dépassement du budget : ").append(overBudget ? YES : NO).append("\n");
        sb.append("- Budget presque consommé (>=80%) : ").append(budgetAlmostFull ? YES : NO).append("\n");

        if (!currentBudgets.isEmpty()) {
            sb.append("- Détail budgets :\n");
            for (Budget budget : currentBudgets) {
                sb.append("  • ")
                        .append(budget.getCategory() == null || budget.getCategory().isBlank() ? "GLOBAL" : budget.getCategory())
                        .append(" : ")
                        .append(formatMoney(budget.getLimitAmount()))
                        .append(" TND\n");
            }
        }
        sb.append("\n");

        sb.append("6) OBJECTIFS D'ÉPARGNE\n");
        if (activeGoals == null || activeGoals.isEmpty()) {
            sb.append("- Aucun objectif d'épargne actif\n");
        } else {
            for (SavingsGoal goal : activeGoals) {
                sb.append("- ")
                        .append(valueOrNd(goal.getName()))
                        .append(" | Progression : ")
                        .append(formatMoney(goal.getCurrentAmount()))
                        .append("/")
                        .append(formatMoney(goal.getTargetAmount()))
                        .append(" TND")
                        .append(" | Deadline : ")
                        .append(goal.getDeadline() != null ? formatDate(goal.getDeadline()) : ND)
                        .append(" | Objectif hebdo : ")
                        .append(formatMoney(goal.getWeeklyTarget()))
                        .append(" TND\n");
            }
        }
        sb.append("\n");

        sb.append("7) INTERPRÉTATION PRODUIT POUR LE COACH\n");
        sb.append("- Alerte principale : ").append(mainAlert).append("\n");
        sb.append("- Point positif principal : ").append(mainPositive).append("\n");
        sb.append("- Consigne importante : ");
        if (!hasEnoughHistoryForTrend) {
            sb.append("Ne pas conclure sur une trajectoire stable à long terme. ");
        }
        if (!burnRateUsable) {
            sb.append("Ne pas interpréter un burn rate nul comme une preuve de stabilité. ");
        }
        if (!runwayUsable) {
            sb.append("Éviter les affirmations trop certaines sur la durée de survie financière. ");
        }
        sb.append("Toujours expliquer simplement et proposer une action concrète.\n");

        sb.append("=== FIN DU CONTEXTE ===");

        return sb.toString();
    }

    private BurnRateDto.Response safeBurnRate(Long userId) {
        try {
            return burnRateService.compute(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private long getMonthsOfHistory(Long userId) {
        try {
            return cashFlowSummaryRepository.countMonthsOfHistoryByUserId(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal calculateBudgetUsage(BigDecimal totalExpense, BigDecimal budgetLimit) {
        if (budgetLimit == null || budgetLimit.compareTo(BigDecimal.ZERO) <= 0 || totalExpense == null) {
            return null;
        }

        return totalExpense
                .divide(budgetLimit, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBurnRateUsable(BigDecimal burnRateAmount, String burnRateStatus, long monthsOfHistory) {
        if (burnRateAmount == null) return false;
        if (burnRateStatus == null || burnRateStatus.isBlank()) return false;
        if ("NO_DATA".equalsIgnoreCase(burnRateStatus)) return false;
        if (monthsOfHistory < 1) return false;
        return burnRateAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildMainAlert(long openRiskCases,
                                  boolean runwayUsable,
                                  BigDecimal runwayMonths,
                                  boolean overBudget,
                                  boolean burnRateUsable,
                                  boolean hasEnoughHistoryForTrend) {

        if (openRiskCases > 0) {
            return "Des cas de risque sont ouverts ; la situation doit être surveillée de près.";
        }

        if (runwayUsable && runwayMonths != null && runwayMonths.compareTo(new BigDecimal("2")) < 0) {
            return "Le runway est critique : priorité immédiate à la trésorerie.";
        }

        if (overBudget) {
            return "Le budget mensuel est dépassé.";
        }

        if (!burnRateUsable || !hasEnoughHistoryForTrend) {
            return "L'historique est encore trop limité pour une lecture fiable de la trajectoire.";
        }

        return "Aucune alerte critique immédiate détectée.";
    }

    private String buildMainPositive(boolean shortTermPositive,
                                     boolean budgetExists,
                                     BigDecimal budgetUsagePercent,
                                     String badgeLevel,
                                     List<SavingsGoal> activeGoals) {

        if (shortTermPositive) {
            return "Le mois en cours présente un flux net positif.";
        }

        if (budgetExists && budgetUsagePercent != null && budgetUsagePercent.compareTo(new BigDecimal("50")) < 0) {
            return "Les dépenses restent bien contenues par rapport au budget.";
        }

        if (badgeLevel != null && !badgeLevel.isBlank() && !"UNVERIFIED".equalsIgnoreCase(badgeLevel)) {
            return "Le profil de confiance financière commence à se structurer.";
        }

        if (activeGoals != null && !activeGoals.isEmpty()) {
            return "Des objectifs d'épargne sont en place, ce qui facilite les conseils orientés action.";
        }

        return "Le contexte contient déjà assez d'informations pour donner un premier accompagnement.";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return ND;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) return ND;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return ND;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String formatDate(LocalDate date) {
        if (date == null) return ND;
        return date.format(DATE_FMT);
    }

    private String formatMonth(LocalDate date) {
        if (date == null) return ND;
        return date.format(MONTH_FMT);
    }

    private String valueOrNd(String value) {
        return value == null || value.isBlank() ? ND : value;
    }
}