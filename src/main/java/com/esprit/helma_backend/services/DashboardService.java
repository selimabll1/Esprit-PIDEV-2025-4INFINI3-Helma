package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final CashFlowService cashFlowService;
    private final BurnRateService burnRateService;
    private final CashFlowForecastService forecastService;
    private final FinancialHealthScoreService healthScoreService;
    private final TrustBadgeService trustBadgeService;
    private final IncomeStatementService incomeStatementService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;
    private final RiskCaseService riskCaseService;

    public DashboardService(CashFlowService cashFlowService,
                            BurnRateService burnRateService,
                            CashFlowForecastService forecastService,
                            FinancialHealthScoreService healthScoreService,
                            TrustBadgeService trustBadgeService,
                            IncomeStatementService incomeStatementService,
                            BudgetService budgetService,
                            SavingsGoalService savingsGoalService,
                            RiskCaseService riskCaseService) {
        this.cashFlowService = cashFlowService;
        this.burnRateService = burnRateService;
        this.forecastService = forecastService;
        this.healthScoreService = healthScoreService;
        this.trustBadgeService = trustBadgeService;
        this.incomeStatementService = incomeStatementService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
        this.riskCaseService = riskCaseService;
    }

    public DashboardDto.Response getDashboard(Long userId) {
        List<CashFlowDto.Response> history = safeCashFlowHistory(userId);
        CashFlowDto.Response referenceCashFlow = resolveReferenceCashFlow(userId, history);

        BurnRateDto.Response burnRate = safeBurnRate(userId);
        ForecastDto.Response forecast = safeForecast(userId);
        HealthScoreDto.Response health = safeHealth(userId);
        TrustBadgeDto.Response badge = safeBadge(userId);
        IncomeStatementDto.Response incomeStatement = safeIncomeStatement(userId);

        LocalDate referenceMonth = referenceCashFlow != null && referenceCashFlow.monthStart() != null
                ? referenceCashFlow.monthStart()
                : LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);

        List<BudgetDto.Response> budgets = safeBudgets(userId, referenceMonth);
        List<SavingsGoalDto.Response> goals = safeGoals(userId);
        List<RiskCaseDto.Response> openRisks = safeOpenRisks(userId);

        String mainAlert = deriveMainAlert(health, burnRate, forecast, openRisks);
        String mainPositive = deriveMainPositive(referenceCashFlow, badge);
        String nextBestAction = deriveNextAction(health, burnRate, budgets, goals);

        return new DashboardDto.Response(
                userId,
                referenceCashFlow,
                burnRate,
                forecast,
                health,
                badge,
                incomeStatement,
                budgets,
                goals,
                openRisks,
                mainAlert,
                mainPositive,
                nextBestAction
        );
    }

    private List<CashFlowDto.Response> safeCashFlowHistory(Long userId) {
        try {
            return cashFlowService.getHistory(userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private CashFlowDto.Response resolveReferenceCashFlow(Long userId, List<CashFlowDto.Response> history) {
        if (history != null && !history.isEmpty()) {
            return history.stream()
                    .filter(h -> h.monthStart() != null)
                    .max(Comparator.comparing(CashFlowDto.Response::monthStart))
                    .orElse(history.get(history.size() - 1));
        }

        try {
            return cashFlowService.getCurrentMonth(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private BurnRateDto.Response safeBurnRate(Long userId) {
        try {
            return burnRateService.compute(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private ForecastDto.Response safeForecast(Long userId) {
        try {
            return forecastService.forecast(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private HealthScoreDto.Response safeHealth(Long userId) {
        try {
            return healthScoreService.compute(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private TrustBadgeDto.Response safeBadge(Long userId) {
        try {
            return trustBadgeService.compute(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private IncomeStatementDto.Response safeIncomeStatement(Long userId) {
        try {
            return incomeStatementService.getCurrent(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<BudgetDto.Response> safeBudgets(Long userId, LocalDate monthStart) {
        try {
            return budgetService.getByUserAndMonth(userId, monthStart);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SavingsGoalDto.Response> safeGoals(Long userId) {
        try {
            return savingsGoalService.getByUser(userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RiskCaseDto.Response> safeOpenRisks(Long userId) {
        try {
            return riskCaseService.getByUser(userId).stream()
                    .filter(r -> "OPEN".equalsIgnoreCase(r.status()))
                    .sorted(Comparator.comparing(RiskCaseDto.Response::detectedAt).reversed())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isHealthUnavailable(HealthScoreDto.Response health) {
        return health == null
                || health.score() == null
                || health.label() == null
                || "-".equals(health.label());
    }

    private boolean isBurnUnavailable(BurnRateDto.Response burnRate) {
        return burnRate == null
                || burnRate.runwayMonths() == null
                || burnRate.status() == null
                || "NO_DATA".equalsIgnoreCase(burnRate.burnRateMethod());
    }

    private String deriveMainAlert(HealthScoreDto.Response health,
                                   BurnRateDto.Response burnRate,
                                   ForecastDto.Response forecast,
                                   List<RiskCaseDto.Response> openRisks) {

        if (openRisks != null && !openRisks.isEmpty()) {
            return "Des risques ouverts demandent une action rapide.";
        }

        if (forecast != null && forecast.projectedCashoutDate() != null) {
            return "Risque de manque de trésorerie autour de " + forecast.projectedCashoutDate();
        }

        if (burnRate != null
                && burnRate.runwayMonths() != null
                && burnRate.runwayMonths().doubleValue() < 2.0) {
            return "Runway critique : moins de 2 mois de marge.";
        }

        if (health != null && "FRAGILE".equalsIgnoreCase(health.label())) {
            return "Santé financière fragile : surveille les dépenses et la trésorerie.";
        }

        if (isHealthUnavailable(health) && isBurnUnavailable(burnRate)) {
            return "Pas assez de données pour détecter une alerte fiable.";
        }

        return "Aucune alerte critique pour le moment.";
    }

    private String deriveMainPositive(CashFlowDto.Response cashFlow,
                                      TrustBadgeDto.Response badge) {
        if (cashFlow != null
                && cashFlow.netFlow() != null
                && cashFlow.netFlow().doubleValue() > 0) {
            return "Ton flux net du mois de référence est positif.";
        }

        if (badge != null
                && badge.level() != null
                && !"UNVERIFIED".equalsIgnoreCase(badge.level().name())) {
            return "Ton niveau de confiance actuel est " + badge.level().name() + ".";
        }

        return "Ajoute d'abord des transactions pour construire une lecture financière fiable.";
    }

    private String deriveNextAction(HealthScoreDto.Response health,
                                    BurnRateDto.Response burnRate,
                                    List<BudgetDto.Response> budgets,
                                    List<SavingsGoalDto.Response> goals) {

        if (isHealthUnavailable(health) && isBurnUnavailable(burnRate)) {
            return "Commence par ajouter des transactions sur au moins un mois pour générer les KPI.";
        }

        if (burnRate != null
                && burnRate.runwayMonths() != null
                && burnRate.runwayMonths().doubleValue() < 2.0) {
            return "Réduis immédiatement les dépenses non essentielles et sécurise au moins 2 mois de trésorerie.";
        }

        if (budgets != null && !budgets.isEmpty()) {
            return "Vérifie les catégories qui approchent leur plafond sur le mois de référence.";
        }

        if (goals != null && !goals.isEmpty()) {
            return "Continue le rythme d'épargne sur tes objectifs actifs.";
        }

        if (health != null && "FRAGILE".equalsIgnoreCase(health.label())) {
            return "Construis un budget simple pour mieux cadrer le mois prochain.";
        }

        return "Continue ce rythme et surveille la tendance sur les 3 prochains mois.";
    }
}