package com.esprit.helma_backend.controllers;
import com.esprit.helma_backend.dto.*;
import com.esprit.helma_backend.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "PDF report generation for entrepreneurs")
public class CashFlowReportController {

    private final CashFlowService cashFlowService;
    private final CashFlowForecastService forecastService;
    private final CashFlowPdfReportService pdfService;
    private final UserService userService;
    private final BudgetService budgetService;
    private final SavingsGoalService savingsGoalService;
    private final FinancialHealthScoreService healthScoreService;
    private final BurnRateService burnRateService;
    private final TransactionExcelService excelService;

    public CashFlowReportController(CashFlowService cashFlowService,
                                    CashFlowForecastService forecastService,
                                    CashFlowPdfReportService pdfService,
                                    UserService userService,
                                    BudgetService budgetService,
                                    SavingsGoalService savingsGoalService,
                                    FinancialHealthScoreService healthScoreService,
                                    BurnRateService burnRateService,
                                    TransactionExcelService excelService) {
        this.cashFlowService = cashFlowService;
        this.forecastService = forecastService;
        this.pdfService = pdfService;
        this.userService = userService;
        this.budgetService = budgetService;
        this.savingsGoalService = savingsGoalService;
        this.healthScoreService = healthScoreService;
        this.burnRateService = burnRateService;
        this.excelService = excelService;
    }

    /* ── Original endpoint (kept for backward compatibility) ─────────── */

    @GetMapping("/cash-flow/{userId}")
    @Operation(
            summary = "Download full cash flow + forecast report (PDF)",
            description = "Generates a professional PDF report with full cash flow history and a 3-month forecast."
    )
    public ResponseEntity<byte[]> downloadCashFlowReport(@PathVariable Long userId) {
        UserDto.Response user = userService.getById(userId);
        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId);
        ForecastDto.Response forecast = forecastService.forecast(userId);

        byte[] pdf = pdfService.generate(user.fullName(), history, forecast);

        String filename = "helma-cashflow-analysis-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";

        return pdfResponse(pdf, filename);
    }

    /* ── Monthly report for a specific month ─────────────────────────── */

    @GetMapping("/monthly/{userId}")
    @Operation(
            summary = "Download monthly financial analysis (PDF)",
            description = "Generates a detailed PDF report for a specific month: cash flow, budget vs actual, savings, risk, health score."
    )
    public ResponseEntity<byte[]> downloadMonthlyReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        LocalDate monthStart = month.withDayOfMonth(1);

        UserDto.Response user = userService.getById(userId);
        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId);
        List<BudgetDto.Response> budgets = budgetService.getByUserAndMonth(userId, monthStart);
        List<SavingsGoalDto.Response> goals = savingsGoalService.getByUser(userId);
        HealthScoreDto.Response healthScore = safeHealthScore(userId);
        BurnRateDto.Response burnRate = safeBurnRate(userId);

        byte[] pdf = pdfService.generateMonthly(
                user.fullName(), monthStart, history, budgets, goals, healthScore, burnRate);

        String filename = "helma-monthly-" + monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf";

        return pdfResponse(pdf, filename);
    }

    /* ── Yearly report ───────────────────────────────────────────────── */

    @GetMapping("/yearly/{userId}")
    @Operation(
            summary = "Download yearly financial summary (PDF)",
            description = "Generates a comprehensive PDF report for an entire year: month-by-month breakdown, totals, averages, trends."
    )
    public ResponseEntity<byte[]> downloadYearlyReport(
            @PathVariable Long userId,
            @RequestParam int year) {

        UserDto.Response user = userService.getById(userId);
        List<CashFlowDto.Response> fullHistory = cashFlowService.getHistory(userId);
        List<BudgetDto.Response> allBudgets = budgetService.getByUser(userId);
        List<SavingsGoalDto.Response> goals = savingsGoalService.getByUser(userId);
        HealthScoreDto.Response healthScore = safeHealthScore(userId);
        ForecastDto.Response forecast = forecastService.forecast(userId);

        byte[] pdf = pdfService.generateYearly(
                user.fullName(), year, fullHistory, allBudgets, goals, healthScore, forecast);

        String filename = "helma-yearly-" + year + ".pdf";

        return pdfResponse(pdf, filename);
    }

    /* ── Excel export ────────────────────────────────────────────────── */

    @GetMapping("/transactions-excel/{userId}")
    @Operation(
            summary = "Export transactions as Excel (.xlsx)",
            description = "Generates a formatted Excel file with all transactions for a given month."
    )
    public ResponseEntity<byte[]> downloadTransactionsExcel(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        byte[] excel = excelService.generate(userId, month);

        String filename = "helma-transactions-" +
                month.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excel);
    }

    /* ── List available months (for the frontend dropdown) ───────────── */

    @GetMapping("/available-months/{userId}")
    @Operation(
            summary = "List months that have data",
            description = "Returns a list of months (yyyy-MM-dd) for which cash flow data exists, to populate the frontend dropdown."
    )
    public ResponseEntity<List<String>> getAvailableMonths(@PathVariable Long userId) {
        List<CashFlowDto.Response> history = cashFlowService.getHistory(userId);
        List<String> months = history.stream()
                .map(h -> h.monthStart().toString())
                .toList();
        return ResponseEntity.ok(months);
    }

    /* ── Helpers ──────────────────────────────────────────────────────── */

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    private HealthScoreDto.Response safeHealthScore(Long userId) {
        try {
            return healthScoreService.compute(userId);
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
}