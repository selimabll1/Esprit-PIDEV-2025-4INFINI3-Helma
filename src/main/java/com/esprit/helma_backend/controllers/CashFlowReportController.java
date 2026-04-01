package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.*;
import com.esprit.helma_backend.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "PDF report generation for entrepreneurs")
public class CashFlowReportController {

    private final CashFlowService cashFlowService;
    private final CashFlowForecastService forecastService;
    private final CashFlowPdfReportService pdfService;
    private final UserService userService;
    private final BurnRateService burnRateService;
    private final FinancialHealthScoreService healthScoreService;
    private final TrustBadgeService trustBadgeService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;
    private final RiskCaseService riskCaseService;

    public CashFlowReportController(CashFlowService cashFlowService,
                                    CashFlowForecastService forecastService,
                                    CashFlowPdfReportService pdfService,
                                    UserService userService,
                                    BurnRateService burnRateService,
                                    FinancialHealthScoreService healthScoreService,
                                    TrustBadgeService trustBadgeService,
                                    BudgetService budgetService,
                                    SavingsGoalService savingsGoalService,
                                    RiskCaseService riskCaseService) {
        this.cashFlowService = cashFlowService;
        this.forecastService = forecastService;
        this.pdfService = pdfService;
        this.userService = userService;
        this.burnRateService = burnRateService;
        this.healthScoreService = healthScoreService;
        this.trustBadgeService = trustBadgeService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
        this.riskCaseService = riskCaseService;
    }

    @GetMapping("/cash-flow/{userId}")
    @Operation(
            summary = "Download monthly entrepreneur brief (PDF)",
            description = "Generates a richer monthly PDF report with analysis, forecast, score, badge, budgets, goals, risks, and demo financing packs."
    )
    public ResponseEntity<byte[]> downloadCashFlowReport(@PathVariable Long userId) {
        UserDto.Response user = userService.getById(userId);

        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId).stream()
                .sorted(Comparator.comparing(CashFlowDto.Response::monthStart))
                .toList();

        ForecastDto.Response forecast = forecastService.forecast(userId);
        BurnRateDto.Response burnRate = burnRateService.compute(userId);
        HealthScoreDto.Response health = healthScoreService.compute(userId);
        TrustBadgeDto.Response badge = trustBadgeService.compute(userId);

        LocalDate referenceMonth = history.isEmpty()
                ? LocalDate.now().withDayOfMonth(1)
                : history.get(history.size() - 1).monthStart();

        List<BudgetDto.Response> monthBudgets = budgetService.getByUserAndMonth(userId, referenceMonth);
        List<SavingsGoalDto.Response> goals = savingsGoalService.getByUser(userId);
        List<RiskCaseDto.Response> openRisks = riskCaseService.getByUser(userId).stream()
                .filter(r -> "OPEN".equalsIgnoreCase(r.status()))
                .sorted(Comparator.comparing(RiskCaseDto.Response::detectedAt).reversed())
                .toList();

        byte[] pdf = pdfService.generate(
                user.fullName(),
                referenceMonth,
                history,
                forecast,
                burnRate,
                health,
                badge,
                monthBudgets,
                goals,
                openRisks
        );

        String filename = "helma-monthly-brief-" +
                referenceMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }
}