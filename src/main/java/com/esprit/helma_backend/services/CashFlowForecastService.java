package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.CashFlowDto;
import com.esprit.helma_backend.dto.ForecastDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CashFlowForecastService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CashFlowService cashFlowService;

    public CashFlowForecastService(CashFlowService cashFlowService) {
        this.cashFlowService = cashFlowService;
    }

    public ForecastDto.Response forecast(Long userId) {
        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId);

        if (history == null || history.isEmpty()) {
            return new ForecastDto.Response(
                    userId,
                    0,
                    "NO_DATA",
                    "VERY_LOW",
                    "INSUFFICIENT",
                    "UNKNOWN",
                    "UNKNOWN",
                    null,
                    null,
                    ZERO.setScale(2, RoundingMode.HALF_UP),
                    ZERO.setScale(2, RoundingMode.HALF_UP),
                    ZERO.setScale(2, RoundingMode.HALF_UP),
                    null,
                    null,
                    List.of(),
                    List.of("Pas assez d'historique pour générer une prévision."),
                    List.of("Aucune donnée mensuelle disponible."),
                    LocalDate.now()
            );
        }

        int start = Math.max(0, history.size() - 6);
        List<CashFlowDto.Response> sample = history.subList(start, history.size());
        int n = sample.size();

        List<BigDecimal> incomes = sample.stream()
                .map(CashFlowDto.Response::totalIncome)
                .map(this::nz)
                .toList();

        List<BigDecimal> expenses = sample.stream()
                .map(CashFlowDto.Response::totalExpense)
                .map(this::nz)
                .toList();

        BigDecimal latestBalance = nz(history.get(history.size() - 1).cumulativeBalance());
        LocalDate latestMonth = history.get(history.size() - 1).monthStart();

        Regression incomeReg = fit(incomes);
        Regression expenseReg = fit(expenses);

        String forecastMethod = resolveMethod(n);
        String confidenceLevel = resolveConfidence(n);
        String historyQuality = resolveHistoryQuality(n);

        String incomeTrend = trendLabel(incomeReg.slope());
        String expenseTrend = trendLabel(expenseReg.slope());

        List<ForecastDto.ForecastMonth> months = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        BigDecimal balance = latestBalance;
        BigDecimal incomeSum = ZERO;
        BigDecimal expenseSum = ZERO;
        BigDecimal netSum = ZERO;
        LocalDate projectedCashoutDate = null;

        for (int i = 1; i <= 3; i++) {
            BigDecimal predictedIncome;
            BigDecimal predictedExpense;

            if (n == 1) {
                predictedIncome = nonNegative(incomes.get(0));
                predictedExpense = nonNegative(expenses.get(0));
            } else {
                predictedIncome = nonNegative(incomeReg.predict(n + i));
                predictedExpense = nonNegative(expenseReg.predict(n + i));
            }

            BigDecimal predictedNet = predictedIncome.subtract(predictedExpense).setScale(2, RoundingMode.HALF_UP);

            BigDecimal previousBalance = balance;
            balance = balance.add(predictedNet).setScale(2, RoundingMode.HALF_UP);

            LocalDate month = latestMonth.plusMonths(i);

            months.add(new ForecastDto.ForecastMonth(
                    month,
                    predictedIncome,
                    predictedExpense,
                    predictedNet,
                    balance
            ));

            incomeSum = incomeSum.add(predictedIncome);
            expenseSum = expenseSum.add(predictedExpense);
            netSum = netSum.add(predictedNet);

            if (projectedCashoutDate == null && previousBalance.compareTo(ZERO) > 0 && balance.compareTo(ZERO) <= 0) {
                BigDecimal monthlyNetLoss = predictedExpense.subtract(predictedIncome);

                if (monthlyNetLoss.compareTo(ZERO) > 0) {
                    BigDecimal fraction = previousBalance.divide(monthlyNetLoss, 4, RoundingMode.HALF_UP);
                    long days = Math.max(1, fraction.multiply(BigDecimal.valueOf(30)).longValue());
                    projectedCashoutDate = month.plusDays(Math.min(days, 30));
                } else {
                    projectedCashoutDate = month;
                }

                alerts.add("Risque de tension de trésorerie autour de " + projectedCashoutDate + ".");
            }
        }

        BigDecimal avgPredictedIncome = incomeSum.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal avgPredictedExpense = expenseSum.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal avgPredictedNetFlow = netSum.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        BigDecimal predictedRunwayMonths = null;

        // Better runway logic:
        // - if predicted net flow is positive or zero, runway is not the right limiting metric
        // - otherwise use predicted monthly net loss
        if (avgPredictedNetFlow.compareTo(ZERO) < 0 && latestBalance.compareTo(ZERO) > 0) {
            BigDecimal monthlyNetLoss = avgPredictedNetFlow.abs();
            predictedRunwayMonths = latestBalance.divide(monthlyNetLoss, 2, RoundingMode.HALF_UP);
        }

        buildAlertsAndExplanations(
                n,
                confidenceLevel,
                incomeTrend,
                expenseTrend,
                avgPredictedIncome,
                avgPredictedExpense,
                avgPredictedNetFlow,
                predictedRunwayMonths,
                projectedCashoutDate,
                alerts,
                explanations
        );

        return new ForecastDto.Response(
                userId,
                n,
                forecastMethod,
                confidenceLevel,
                historyQuality,
                incomeTrend,
                expenseTrend,
                incomeReg.slope().setScale(2, RoundingMode.HALF_UP),
                expenseReg.slope().setScale(2, RoundingMode.HALF_UP),
                avgPredictedIncome,
                avgPredictedExpense,
                avgPredictedNetFlow,
                predictedRunwayMonths,
                projectedCashoutDate,
                months,
                alerts,
                explanations,
                LocalDate.now()
        );
    }

    private void buildAlertsAndExplanations(int n,
                                            String confidenceLevel,
                                            String incomeTrend,
                                            String expenseTrend,
                                            BigDecimal avgPredictedIncome,
                                            BigDecimal avgPredictedExpense,
                                            BigDecimal avgPredictedNetFlow,
                                            BigDecimal predictedRunwayMonths,
                                            LocalDate projectedCashoutDate,
                                            List<String> alerts,
                                            List<String> explanations) {

        if (n < 3) {
            alerts.add("Historique limité : la prévision reste prudente.");
            explanations.add("Moins de 3 mois d'historique : le modèle utilise une base peu profonde.");
        } else {
            explanations.add("La prévision s'appuie sur plusieurs mois d'historique et une tendance linéaire.");
        }

        explanations.add("Niveau de confiance : " + confidenceLevel + ".");
        explanations.add("Tendance revenus : " + incomeTrend + ".");
        explanations.add("Tendance dépenses : " + expenseTrend + ".");

        if (avgPredictedNetFlow.compareTo(ZERO) > 0) {
            alerts.add("La prévision indique un flux net moyen positif sur les 3 prochains mois.");
            explanations.add("Les revenus prévus couvrent les dépenses prévues à court terme.");
        } else if (avgPredictedNetFlow.compareTo(ZERO) < 0) {
            alerts.add("La prévision indique une pression de trésorerie avec un flux net moyen négatif.");
            explanations.add("Les dépenses prévues dépassent les revenus prévus à court terme.");
        } else {
            alerts.add("La prévision indique un équilibre fragile entre revenus et dépenses.");
        }

        if ("RISING".equals(expenseTrend)) {
            alerts.add("Les dépenses montrent une tendance à la hausse.");
        }

        if ("FALLING".equals(incomeTrend)) {
            alerts.add("Les revenus montrent une tendance à la baisse.");
        }

        if (predictedRunwayMonths != null && predictedRunwayMonths.compareTo(new BigDecimal("2")) < 0) {
            alerts.add("Runway prévisionnel inférieur à 2 mois.");
        }

        if (projectedCashoutDate == null && avgPredictedNetFlow.compareTo(ZERO) >= 0) {
            explanations.add("Aucune rupture de trésorerie projetée à horizon 3 mois.");
        }
    }

    private String resolveMethod(int n) {
        if (n <= 0) return "NO_DATA";
        if (n == 1) return "LAST_MONTH_FLAT";
        if (n == 2) return "LINEAR_REGRESSION_LOW_HISTORY";
        return "LINEAR_REGRESSION";
    }

    private String resolveConfidence(int n) {
        if (n <= 1) return "VERY_LOW";
        if (n == 2) return "LOW";
        if (n == 3) return "MEDIUM";
        if (n <= 5) return "GOOD";
        return "HIGH";
    }

    private String resolveHistoryQuality(int n) {
        if (n <= 1) return "INSUFFICIENT";
        if (n == 2) return "LIMITED";
        if (n == 3) return "USABLE";
        return "STRONG";
    }

    private String trendLabel(BigDecimal slope) {
        if (slope == null) return "UNKNOWN";

        BigDecimal threshold = new BigDecimal("25");

        if (slope.compareTo(threshold) > 0) return "RISING";
        if (slope.compareTo(threshold.negate()) < 0) return "FALLING";
        return "STABLE";
    }

    private Regression fit(List<BigDecimal> values) {
        int n = values.size();

        if (n == 1) {
            return new Regression(ZERO, values.get(0));
        }

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = values.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator = n * sumXX - sumX * sumX;
        double slope = denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        return new Regression(BigDecimal.valueOf(slope), BigDecimal.valueOf(intercept));
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value.compareTo(ZERO) < 0) return ZERO.setScale(2, RoundingMode.HALF_UP);
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private record Regression(BigDecimal slope, BigDecimal intercept) {
        BigDecimal predict(int x) {
            return slope.multiply(BigDecimal.valueOf(x)).add(intercept);
        }
    }
}