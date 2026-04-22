package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CashFlowPdfReportService {

    private static final Color BRAND_DARK = new Color(15, 15, 20);
    private static final Color BRAND_PURPLE = new Color(83, 74, 183);
    private static final Color BRAND_SOFT = new Color(230, 230, 245);
    private static final Color BRAND_TEAL = new Color(15, 107, 104);
    private static final Color BRAND_GOLD = new Color(212, 166, 42);
    private static final Color POSITIVE = new Color(15, 110, 86);
    private static final Color NEGATIVE = new Color(163, 45, 45);
    private static final Color HEADER_BG = new Color(34, 34, 40);
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color WARNING_BG = new Color(255, 247, 224);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /* ══════════════════════════════════════════════════════════════════
       ORIGINAL — full history + forecast report (unchanged)
       ══════════════════════════════════════════════════════════════════ */

    public byte[] generate(String userName,
                           List<CashFlowDto.Response> history,
                           ForecastDto.Response forecast) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 46);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterEvent(userName, "Cash Flow Analysis"));
            document.open();

            addCover(document, userName, history, forecast);
            document.newPage();
            addSectionTitle(document, "1. Executive Summary");
            addExecutiveSummary(document, history, forecast);
            addSectionTitle(document, "2. Historical Cash Flow Analysis");
            addHistoryTable(document, history);
            addSectionTitle(document, "3. Forecast for the Next 3 Months");
            addForecastTable(document, forecast);
            addSectionTitle(document, "4. Entrepreneur Recommendations");
            addRecommendations(document, history, forecast);
            addSectionTitle(document, "5. Methodology");
            addMethodology(document, forecast);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        }
    }

    /* ══════════════════════════════════════════════════════════════════
       MONTHLY — detailed single-month analysis
       ══════════════════════════════════════════════════════════════════ */

    public byte[] generateMonthly(String userName,
                                  LocalDate monthStart,
                                  List<CashFlowDto.Response> fullHistory,
                                  List<BudgetDto.Response> budgets,
                                  List<SavingsGoalDto.Response> goals,
                                  HealthScoreDto.Response healthScore,
                                  BurnRateDto.Response burnRate) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 54, 46);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            String reportTitle = "Monthly Analysis — " + monthStart.format(MONTH_FMT);
            writer.setPageEvent(new HeaderFooterEvent(userName, reportTitle));
            doc.open();

            // ── Cover ────────────────────────────────────────────────
            addBrandBanner(doc, "HELMA\nMonthly Financial Analysis", reportTitle);
            doc.add(Chunk.NEWLINE);
            doc.add(line("Prepared for", userName));
            doc.add(line("Analysis period", monthStart.format(MONTH_FMT)));
            doc.add(line("Generated on", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH))));
            doc.add(Chunk.NEWLINE);

            // ── Find this month's cash flow ──────────────────────────
            CashFlowDto.Response monthCf = fullHistory.stream()
                    .filter(cf -> cf.monthStart() != null && cf.monthStart().equals(monthStart))
                    .findFirst().orElse(null);

            // ── 1. Cash Flow Summary ─────────────────────────────────
            doc.newPage();
            addSectionTitle(doc, "1. Cash Flow Summary — " + monthStart.format(MONTH_FMT));
            if (monthCf != null) {
                PdfPTable kpi = new PdfPTable(4);
                kpi.setWidthPercentage(100);
                kpi.setSpacingAfter(14f);
                kpi.setWidths(new float[]{1f, 1f, 1f, 1f});
                kpi.addCell(kpiCell("Total Income", money(monthCf.totalIncome()), POSITIVE));
                kpi.addCell(kpiCell("Total Expense", money(monthCf.totalExpense()), NEGATIVE));
                kpi.addCell(kpiCell("Net Flow", money(monthCf.netFlow()), nz(monthCf.netFlow()).signum() >= 0 ? POSITIVE : NEGATIVE));
                kpi.addCell(kpiCell("Cumulative Balance", money(monthCf.cumulativeBalance()), nz(monthCf.cumulativeBalance()).signum() >= 0 ? POSITIVE : NEGATIVE));
                doc.add(kpi);

                // Comparison with previous month
                CashFlowDto.Response prevCf = fullHistory.stream()
                        .filter(cf -> cf.monthStart() != null && cf.monthStart().isBefore(monthStart))
                        .reduce((a, b) -> b).orElse(null);

                if (prevCf != null) {
                    BigDecimal incomeChange = pctChange(prevCf.totalIncome(), monthCf.totalIncome());
                    BigDecimal expenseChange = pctChange(prevCf.totalExpense(), monthCf.totalExpense());
                    Paragraph comparison = new Paragraph();
                    comparison.setLeading(18f);
                    comparison.add(new Chunk("Month-over-month: ", font(11, Font.BOLD, BRAND_DARK)));
                    comparison.add(new Chunk("Income " + formatChange(incomeChange) + " | Expense " + formatChange(expenseChange),
                            font(11, Font.NORMAL, BRAND_DARK)));
                    doc.add(comparison);
                    doc.add(Chunk.NEWLINE);
                }
            } else {
                doc.add(new Paragraph("No cash flow data available for " + monthStart.format(MONTH_FMT) + ".",
                        font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            // ── 2. Budget vs Actual ──────────────────────────────────
            addSectionTitle(doc, "2. Budget vs Actual");
            if (budgets != null && !budgets.isEmpty()) {
                PdfPTable budgetTable = new PdfPTable(3);
                budgetTable.setWidthPercentage(100);
                budgetTable.setSpacingAfter(12f);
                budgetTable.setWidths(new float[]{1.5f, 1f, 1f});
                addHeader(budgetTable, "Category");
                addHeader(budgetTable, "Budget Limit");
                addHeader(budgetTable, "Status");

                BigDecimal totalLimit = BigDecimal.ZERO;
                for (BudgetDto.Response b : budgets) {
                    addCell(budgetTable, b.category() != null ? b.category() : "General");
                    addCell(budgetTable, money(b.limitAmount()));
                    addCell(budgetTable, "Active");
                    totalLimit = totalLimit.add(nz(b.limitAmount()));
                }
                doc.add(budgetTable);

                BigDecimal totalSpent = monthCf != null ? nz(monthCf.totalExpense()) : BigDecimal.ZERO;
                BigDecimal usagePct = totalLimit.compareTo(BigDecimal.ZERO) > 0
                        ? totalSpent.divide(totalLimit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

                PdfPTable usageKpi = new PdfPTable(3);
                usageKpi.setWidthPercentage(100);
                usageKpi.setSpacingAfter(14f);
                usageKpi.setWidths(new float[]{1f, 1f, 1f});
                usageKpi.addCell(kpiCell("Total Budget", money(totalLimit), BRAND_TEAL));
                usageKpi.addCell(kpiCell("Total Spent", money(totalSpent), totalSpent.compareTo(totalLimit) > 0 ? NEGATIVE : BRAND_DARK));
                Color usageColor = usagePct.compareTo(new BigDecimal("100")) > 0 ? NEGATIVE
                        : usagePct.compareTo(new BigDecimal("80")) >= 0 ? BRAND_GOLD : POSITIVE;
                usageKpi.addCell(kpiCell("Usage", usagePct.setScale(1, RoundingMode.HALF_UP) + "%", usageColor));
                doc.add(usageKpi);
            } else {
                doc.add(new Paragraph("No budgets were configured for this month.", font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            // ── 3. Financial Health Score ─────────────────────────────
            addSectionTitle(doc, "3. Financial Health Score");
            if (healthScore != null) {
                PdfPTable hsTable = new PdfPTable(2);
                hsTable.setWidthPercentage(100);
                hsTable.setSpacingAfter(12f);
                hsTable.setWidths(new float[]{1f, 1f});
                Color scoreColor = nz(healthScore.score()).compareTo(new BigDecimal("70")) >= 0 ? POSITIVE
                        : nz(healthScore.score()).compareTo(new BigDecimal("50")) >= 0 ? BRAND_GOLD : NEGATIVE;
                hsTable.addCell(kpiCell("Overall Score", healthScore.score().setScale(0, RoundingMode.HALF_UP) + " / 100", scoreColor));
                hsTable.addCell(kpiCell("Status", healthScore.label() != null ? healthScore.label() : "—", scoreColor));
                doc.add(hsTable);

                PdfPTable breakdown = new PdfPTable(5);
                breakdown.setWidthPercentage(100);
                breakdown.setSpacingAfter(14f);
                breakdown.setWidths(new float[]{1f, 1f, 1f, 1f, 1f});
                breakdown.addCell(kpiCell("Runway", score(healthScore.runwayScore()), BRAND_DARK));
                breakdown.addCell(kpiCell("Budget", score(healthScore.budgetScore()), BRAND_DARK));
                breakdown.addCell(kpiCell("Savings", score(healthScore.savingsScore()), BRAND_DARK));
                breakdown.addCell(kpiCell("Stability", score(healthScore.stabilityScore()), BRAND_DARK));
                breakdown.addCell(kpiCell("Risk", score(healthScore.riskScore()), BRAND_DARK));
                doc.add(breakdown);
            } else {
                doc.add(new Paragraph("Health score not yet available — not enough history.", font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            // ── 4. Burn Rate & Runway ────────────────────────────────
            addSectionTitle(doc, "4. Burn Rate & Runway");
            if (burnRate != null) {
                PdfPTable brTable = new PdfPTable(4);
                brTable.setWidthPercentage(100);
                brTable.setSpacingAfter(14f);
                brTable.setWidths(new float[]{1f, 1f, 1f, 1f});
                brTable.addCell(kpiCell("Burn Rate", money(burnRate.burnRate()) + "/mo", BRAND_DARK));
                brTable.addCell(kpiCell("Balance", money(burnRate.currentBalance()), nz(burnRate.currentBalance()).signum() >= 0 ? POSITIVE : NEGATIVE));
                Color statusColor = "CRITICAL".equals(burnRate.status().name()) ? NEGATIVE
                        : "WARNING".equals(burnRate.status().name()) ? BRAND_GOLD : POSITIVE;
                brTable.addCell(kpiCell("Status", burnRate.status().name(), statusColor));
                brTable.addCell(kpiCell("Runway", burnRate.runwayMonths() != null ? burnRate.runwayMonths().setScale(1, RoundingMode.HALF_UP) + " months" : "N/A", statusColor));
                doc.add(brTable);

                if (burnRate.projectedZeroDate() != null) {
                    addWarningBox(doc, "Projected cash-out date: " + burnRate.projectedZeroDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)));
                }

                if (burnRate.finCoachTips() != null && !burnRate.finCoachTips().isEmpty()) {
                    Paragraph tipsTitle = new Paragraph("AI Coach Tips:", font(11, Font.BOLD, BRAND_PURPLE));
                    tipsTitle.setSpacingBefore(8f);
                    doc.add(tipsTitle);
                    com.lowagie.text.List tips = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
                    tips.setIndentationLeft(14f);
                    for (String tip : burnRate.finCoachTips()) {
                        tips.add(new ListItem(tip, font(10, Font.NORMAL, BRAND_DARK)));
                    }
                    doc.add(tips);
                    doc.add(Chunk.NEWLINE);
                }
            } else {
                doc.add(new Paragraph("Burn rate not yet available.", font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            // ── 5. Savings Goals Progress ────────────────────────────
            addSectionTitle(doc, "5. Savings Goals Progress");
            if (goals != null && !goals.isEmpty()) {
                PdfPTable goalTable = new PdfPTable(5);
                goalTable.setWidthPercentage(100);
                goalTable.setSpacingAfter(12f);
                goalTable.setWidths(new float[]{1.5f, 1f, 1f, 0.8f, 0.8f});
                addHeader(goalTable, "Goal");
                addHeader(goalTable, "Progress");
                addHeader(goalTable, "Target");
                addHeader(goalTable, "Deadline");
                addHeader(goalTable, "Status");

                for (SavingsGoalDto.Response g : goals) {
                    addCell(goalTable, g.name() != null ? g.name() : "—");
                    addCell(goalTable, money(g.currentAmount()));
                    addCell(goalTable, money(g.targetAmount()));
                    addCell(goalTable, g.deadline() != null ? g.deadline().toString() : "—");
                    addCell(goalTable, Boolean.TRUE.equals(g.completed()) ? "Completed" : "In progress",
                            Boolean.TRUE.equals(g.completed()) ? POSITIVE : BRAND_DARK);
                }
                doc.add(goalTable);
            } else {
                doc.add(new Paragraph("No savings goals configured.", font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate monthly PDF", e);
        }
    }

    /* ══════════════════════════════════════════════════════════════════
       YEARLY — full year summary
       ══════════════════════════════════════════════════════════════════ */

    public byte[] generateYearly(String userName,
                                 int year,
                                 List<CashFlowDto.Response> fullHistory,
                                 List<BudgetDto.Response> allBudgets,
                                 List<SavingsGoalDto.Response> goals,
                                 HealthScoreDto.Response healthScore,
                                 ForecastDto.Response forecast) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 36, 36, 54, 46);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            String reportTitle = "Annual Report — " + year;
            writer.setPageEvent(new HeaderFooterEvent(userName, reportTitle));
            doc.open();

            // Filter history to this year
            List<CashFlowDto.Response> yearHistory = fullHistory.stream()
                    .filter(cf -> cf.monthStart() != null && cf.monthStart().getYear() == year)
                    .toList();

            List<BudgetDto.Response> yearBudgets = allBudgets.stream()
                    .filter(b -> b.monthStart() != null && LocalDate.parse(b.monthStart().toString()).getYear() == year)
                    .toList();

            // ── Cover ────────────────────────────────────────────────
            addBrandBanner(doc, "HELMA\nAnnual Financial Report", "Fiscal Year " + year);
            doc.add(Chunk.NEWLINE);
            doc.add(line("Prepared for", userName));
            doc.add(line("Fiscal year", String.valueOf(year)));
            doc.add(line("Months with data", String.valueOf(yearHistory.size())));
            doc.add(line("Generated on", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH))));
            doc.add(Chunk.NEWLINE);

            // ── 1. Executive Summary ─────────────────────────────────
            doc.newPage();
            addSectionTitle(doc, "1. Annual Executive Summary");

            BigDecimal totalIncome = yearHistory.stream().map(CashFlowDto.Response::totalIncome).map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = yearHistory.stream().map(CashFlowDto.Response::totalExpense).map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalNet = totalIncome.subtract(totalExpense);
            BigDecimal avgMonthlyIncome = yearHistory.isEmpty() ? BigDecimal.ZERO : totalIncome.divide(BigDecimal.valueOf(yearHistory.size()), 2, RoundingMode.HALF_UP);
            BigDecimal avgMonthlyExpense = yearHistory.isEmpty() ? BigDecimal.ZERO : totalExpense.divide(BigDecimal.valueOf(yearHistory.size()), 2, RoundingMode.HALF_UP);
            BigDecimal marginRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                    ? totalNet.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            PdfPTable annualKpi = new PdfPTable(3);
            annualKpi.setWidthPercentage(100);
            annualKpi.setSpacingAfter(10f);
            annualKpi.setWidths(new float[]{1f, 1f, 1f});
            annualKpi.addCell(kpiCell("Total Annual Income", money(totalIncome), POSITIVE));
            annualKpi.addCell(kpiCell("Total Annual Expense", money(totalExpense), NEGATIVE));
            annualKpi.addCell(kpiCell("Annual Net Result", money(totalNet), totalNet.signum() >= 0 ? POSITIVE : NEGATIVE));
            doc.add(annualKpi);

            PdfPTable avgKpi = new PdfPTable(3);
            avgKpi.setWidthPercentage(100);
            avgKpi.setSpacingAfter(14f);
            avgKpi.setWidths(new float[]{1f, 1f, 1f});
            avgKpi.addCell(kpiCell("Avg Monthly Income", money(avgMonthlyIncome), BRAND_DARK));
            avgKpi.addCell(kpiCell("Avg Monthly Expense", money(avgMonthlyExpense), BRAND_DARK));
            avgKpi.addCell(kpiCell("Annual Margin Rate", marginRate + "%", totalNet.signum() >= 0 ? POSITIVE : NEGATIVE));
            doc.add(avgKpi);

            // ── 2. Month-by-Month Breakdown ──────────────────────────
            addSectionTitle(doc, "2. Month-by-Month Breakdown");

            PdfPTable monthTable = new PdfPTable(5);
            monthTable.setWidthPercentage(100);
            monthTable.setSpacingAfter(12f);
            monthTable.setWidths(new float[]{1.3f, 1.1f, 1.1f, 1.1f, 1.2f});
            addHeader(monthTable, "Month");
            addHeader(monthTable, "Income");
            addHeader(monthTable, "Expense");
            addHeader(monthTable, "Net Flow");
            addHeader(monthTable, "Balance");

            for (CashFlowDto.Response row : yearHistory) {
                addCell(monthTable, row.monthStart() != null ? row.monthStart().format(MONTH_FMT) : "—");
                addCell(monthTable, money(row.totalIncome()));
                addCell(monthTable, money(row.totalExpense()));
                addCell(monthTable, money(row.netFlow()), nz(row.netFlow()).signum() >= 0 ? POSITIVE : NEGATIVE);
                addCell(monthTable, money(row.cumulativeBalance()), nz(row.cumulativeBalance()).signum() >= 0 ? POSITIVE : NEGATIVE);
            }

            // Totals row
            PdfPCell totalLabel = new PdfPCell(new Phrase("ANNUAL TOTAL", font(9, Font.BOLD, Color.WHITE)));
            totalLabel.setBackgroundColor(BRAND_PURPLE);
            totalLabel.setPadding(8f);
            totalLabel.setBorderColor(BRAND_PURPLE);
            monthTable.addCell(totalLabel);
            addTotalCell(monthTable, money(totalIncome), POSITIVE);
            addTotalCell(monthTable, money(totalExpense), NEGATIVE);
            addTotalCell(monthTable, money(totalNet), totalNet.signum() >= 0 ? POSITIVE : NEGATIVE);
            addTotalCell(monthTable, "—", BRAND_DARK);
            doc.add(monthTable);

            // Best / worst months
            if (!yearHistory.isEmpty()) {
                CashFlowDto.Response bestMonth = yearHistory.stream()
                        .max((a, b) -> nz(a.netFlow()).compareTo(nz(b.netFlow()))).orElse(null);
                CashFlowDto.Response worstMonth = yearHistory.stream()
                        .min((a, b) -> nz(a.netFlow()).compareTo(nz(b.netFlow()))).orElse(null);

                if (bestMonth != null && worstMonth != null) {
                    Paragraph p = new Paragraph();
                    p.setLeading(18f);
                    p.add(new Chunk("Best month: ", font(11, Font.BOLD, POSITIVE)));
                    p.add(new Chunk(bestMonth.monthStart().format(MONTH_FMT) + " (net +" + money(bestMonth.netFlow()) + ")", font(11, Font.NORMAL, BRAND_DARK)));
                    p.add(new Chunk("    |    Worst month: ", font(11, Font.BOLD, NEGATIVE)));
                    p.add(new Chunk(worstMonth.monthStart().format(MONTH_FMT) + " (net " + money(worstMonth.netFlow()) + ")", font(11, Font.NORMAL, BRAND_DARK)));
                    doc.add(p);
                    doc.add(Chunk.NEWLINE);
                }
            }

            // ── 3. Budget Discipline ─────────────────────────────────
            addSectionTitle(doc, "3. Annual Budget Discipline");
            if (!yearBudgets.isEmpty()) {
                BigDecimal totalBudgetLimit = yearBudgets.stream()
                        .map(BudgetDto.Response::limitAmount).map(this::nz)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, BigDecimal> budgetByCategory = yearBudgets.stream()
                        .collect(Collectors.groupingBy(
                                b -> b.category() != null ? b.category() : "General",
                                Collectors.reducing(BigDecimal.ZERO, b -> nz(b.limitAmount()), BigDecimal::add)));

                PdfPTable catTable = new PdfPTable(2);
                catTable.setWidthPercentage(100);
                catTable.setSpacingAfter(12f);
                catTable.setWidths(new float[]{1.5f, 1f});
                addHeader(catTable, "Category");
                addHeader(catTable, "Total Budgeted");
                budgetByCategory.forEach((cat, amount) -> {
                    addCell(catTable, cat);
                    addCell(catTable, money(amount));
                });
                doc.add(catTable);

                doc.add(new Paragraph("Total annual budget: " + money(totalBudgetLimit) + " | Total spent: " + money(totalExpense),
                        font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            } else {
                doc.add(new Paragraph("No budgets were configured during " + year + ".", font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);
            }

            // ── 4. Health Score (current snapshot) ───────────────────
            addSectionTitle(doc, "4. Current Financial Health");
            if (healthScore != null) {
                PdfPTable hsKpi = new PdfPTable(6);
                hsKpi.setWidthPercentage(100);
                hsKpi.setSpacingAfter(14f);
                hsKpi.setWidths(new float[]{1f, 1f, 1f, 1f, 1f, 1f});
                Color sc = nz(healthScore.score()).compareTo(new BigDecimal("70")) >= 0 ? POSITIVE
                        : nz(healthScore.score()).compareTo(new BigDecimal("50")) >= 0 ? BRAND_GOLD : NEGATIVE;
                hsKpi.addCell(kpiCell("Overall", score(healthScore.score()), sc));
                hsKpi.addCell(kpiCell("Runway", score(healthScore.runwayScore()), BRAND_DARK));
                hsKpi.addCell(kpiCell("Budget", score(healthScore.budgetScore()), BRAND_DARK));
                hsKpi.addCell(kpiCell("Savings", score(healthScore.savingsScore()), BRAND_DARK));
                hsKpi.addCell(kpiCell("Stability", score(healthScore.stabilityScore()), BRAND_DARK));
                hsKpi.addCell(kpiCell("Risk", score(healthScore.riskScore()), BRAND_DARK));
                doc.add(hsKpi);
            }

            // ── 5. Savings Goals ─────────────────────────────────────
            addSectionTitle(doc, "5. Savings Goals Summary");
            if (goals != null && !goals.isEmpty()) {
                long completed = goals.stream().filter(g -> Boolean.TRUE.equals(g.completed())).count();
                doc.add(new Paragraph(goals.size() + " goals total — " + completed + " completed, " + (goals.size() - completed) + " in progress.",
                        font(11, Font.NORMAL, BRAND_DARK)));
                doc.add(Chunk.NEWLINE);

                PdfPTable goalTable = new PdfPTable(4);
                goalTable.setWidthPercentage(100);
                goalTable.setSpacingAfter(12f);
                goalTable.setWidths(new float[]{1.5f, 1f, 1f, 0.8f});
                addHeader(goalTable, "Goal");
                addHeader(goalTable, "Progress");
                addHeader(goalTable, "Target");
                addHeader(goalTable, "Status");
                for (SavingsGoalDto.Response g : goals) {
                    addCell(goalTable, g.name() != null ? g.name() : "—");
                    addCell(goalTable, money(g.currentAmount()));
                    addCell(goalTable, money(g.targetAmount()));
                    addCell(goalTable, Boolean.TRUE.equals(g.completed()) ? "Done" : "Active",
                            Boolean.TRUE.equals(g.completed()) ? POSITIVE : BRAND_DARK);
                }
                doc.add(goalTable);
            } else {
                doc.add(new Paragraph("No savings goals configured.", font(11, Font.NORMAL, BRAND_DARK)));
            }

            // ── 6. Outlook / Forecast ────────────────────────────────
            if (forecast != null && forecast.months() != null && !forecast.months().isEmpty()) {
                doc.newPage();
                addSectionTitle(doc, "6. Forward-Looking Forecast");
                addForecastTable(doc, forecast);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate yearly PDF", e);
        }
    }

    /* ══════════════════════════════════════════════════════════════════
       SHARED HELPER METHODS
       ══════════════════════════════════════════════════════════════════ */

    private void addBrandBanner(Document doc, String mainTitle, String subtitle) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND_DARK);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(28f);

        Paragraph title = new Paragraph(mainTitle, font(24, Font.BOLD, Color.WHITE));
        title.setLeading(30f);
        cell.addElement(title);

        Paragraph sub = new Paragraph(subtitle, font(11, Font.NORMAL, BRAND_SOFT));
        sub.setSpacingBefore(10f);
        cell.addElement(sub);

        banner.addCell(cell);
        doc.add(banner);
    }

    private void addWarningBox(Document doc, String text) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingAfter(12f);
        PdfPCell cell = new PdfPCell(new Phrase("⚠ " + text, font(10, Font.BOLD, new Color(120, 80, 0))));
        cell.setBackgroundColor(WARNING_BG);
        cell.setBorderColor(BRAND_GOLD);
        cell.setPadding(10f);
        box.addCell(cell);
        doc.add(box);
    }

    private void addTotalCell(PdfPTable table, String text, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.BOLD, color)));
        cell.setPadding(8f);
        cell.setBackgroundColor(LIGHT_BG);
        cell.setBorderColor(new Color(200, 200, 215));
        table.addCell(cell);
    }

    private BigDecimal pctChange(BigDecimal prev, BigDecimal curr) {
        BigDecimal p = nz(prev);
        BigDecimal c = nz(curr);
        if (p.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return c.subtract(p).divide(p, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
    }

    private String formatChange(BigDecimal pct) {
        if (pct.signum() > 0) return "+" + pct + "%";
        return pct + "%";
    }

    private String score(BigDecimal value) {
        return nz(value).setScale(0, RoundingMode.HALF_UP) + "/100";
    }

    /* ── Original methods (kept intact) ─────────────────────────────── */

    private void addCover(Document document, String userName,
                          List<CashFlowDto.Response> history, ForecastDto.Response forecast) throws DocumentException {
        addBrandBanner(document, "HELMA\nMonthly Cash Flow Analysis",
                "Professional financial snapshot and forecast for entrepreneurs");
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
        document.add(line("Prepared for", userName));
        document.add(line("Generated on", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH))));
        document.add(line("History months available", String.valueOf(history.size())));
        document.add(line("Forecast method", safe(forecast != null ? forecast.forecastMethod() : null, "NO_DATA")));
        document.add(line("Confidence", safe(forecast != null ? forecast.confidenceLevel() : null, "LOW")));
        document.add(Chunk.NEWLINE);

        Paragraph intro = new Paragraph(
                "This document gives the entrepreneur a readable monthly analysis of historical cash flow, "
                        + "key current liquidity indicators, and a forward-looking forecast for the next 2–3 months. "
                        + "It is intended to support planning, decision-making, and communication with partners or financiers.",
                font(11, Font.NORMAL, BRAND_DARK));
        intro.setLeading(18f);
        document.add(intro);
    }

    private void addExecutiveSummary(Document document, List<CashFlowDto.Response> history, ForecastDto.Response forecast) throws DocumentException {
        BigDecimal latestBalance = history.isEmpty() ? BigDecimal.ZERO : nz(history.get(history.size() - 1).cumulativeBalance());
        BigDecimal avgIncome = history.isEmpty() ? BigDecimal.ZERO : history.stream().map(CashFlowDto.Response::totalIncome).map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = history.isEmpty() ? BigDecimal.ZERO : history.stream().map(CashFlowDto.Response::totalExpense).map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgNet = history.isEmpty() ? BigDecimal.ZERO : history.stream().map(CashFlowDto.Response::netFlow).map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        PdfPTable kpi = new PdfPTable(4);
        kpi.setWidthPercentage(100);
        kpi.setSpacingAfter(14f);
        kpi.setWidths(new float[]{1f, 1f, 1f, 1f});
        kpi.addCell(kpiCell("Latest Balance", money(latestBalance), latestBalance.signum() >= 0 ? POSITIVE : NEGATIVE));
        kpi.addCell(kpiCell("Avg Monthly Income", money(avgIncome), POSITIVE));
        kpi.addCell(kpiCell("Avg Monthly Expense", money(avgExpense), NEGATIVE));
        kpi.addCell(kpiCell("Avg Net Flow", money(avgNet), avgNet.signum() >= 0 ? POSITIVE : NEGATIVE));
        document.add(kpi);

        Paragraph p = new Paragraph();
        p.setLeading(16f);
        p.add(new Chunk("Forecast outlook: ", font(11, Font.BOLD, BRAND_DARK)));
        p.add(new Chunk(buildOutlookText(history, forecast), font(11, Font.NORMAL, BRAND_DARK)));
        document.add(p);
        document.add(Chunk.NEWLINE);
    }

    private void addHistoryTable(Document document, List<CashFlowDto.Response> history) throws DocumentException {
        if (history == null || history.isEmpty()) {
            document.add(new Paragraph("No cash flow history available.", font(11, Font.NORMAL, BRAND_DARK)));
            document.add(Chunk.NEWLINE);
            return;
        }
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.setWidths(new float[]{1.3f, 1.1f, 1.1f, 1.1f, 1.2f});
        addHeader(table, "Month");
        addHeader(table, "Income");
        addHeader(table, "Expense");
        addHeader(table, "Net");
        addHeader(table, "Balance");
        for (CashFlowDto.Response row : history) {
            addCell(table, row.monthStart() != null ? row.monthStart().format(MONTH_FMT) : "—");
            addCell(table, money(row.totalIncome()));
            addCell(table, money(row.totalExpense()));
            addCell(table, money(row.netFlow()), nz(row.netFlow()).signum() >= 0 ? POSITIVE : NEGATIVE);
            addCell(table, money(row.cumulativeBalance()), nz(row.cumulativeBalance()).signum() >= 0 ? POSITIVE : NEGATIVE);
        }
        document.add(table);
    }

    private void addForecastTable(Document document, ForecastDto.Response forecast) throws DocumentException {
        if (forecast == null || forecast.months() == null || forecast.months().isEmpty()) {
            document.add(new Paragraph("No forecast available.", font(11, Font.NORMAL, BRAND_DARK)));
            document.add(Chunk.NEWLINE);
            return;
        }
        Paragraph meta = new Paragraph(
                "Method: " + safe(forecast.forecastMethod(), "NO_DATA") + "   |   Confidence: " + safe(forecast.confidenceLevel(), "LOW") + "   |   History quality: " + safe(forecast.historyQuality(), "LOW"),
                font(10, Font.NORMAL, BRAND_DARK));
        meta.setSpacingAfter(10f);
        document.add(meta);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.setWidths(new float[]{1.2f, 1.1f, 1.1f, 1.1f, 1.2f});
        addHeader(table, "Forecast Month");
        addHeader(table, "Pred. Income");
        addHeader(table, "Pred. Expense");
        addHeader(table, "Pred. Net");
        addHeader(table, "Pred. Balance");
        for (ForecastDto.ForecastMonth m : forecast.months()) {
            addCell(table, m.monthStart() != null ? m.monthStart().format(MONTH_FMT) : "—");
            addCell(table, money(m.predictedIncome()));
            addCell(table, money(m.predictedExpense()));
            addCell(table, money(m.predictedNetFlow()), nz(m.predictedNetFlow()).signum() >= 0 ? POSITIVE : NEGATIVE);
            addCell(table, money(m.predictedBalance()), nz(m.predictedBalance()).signum() >= 0 ? POSITIVE : NEGATIVE);
        }
        document.add(table);
        if (forecast.projectedCashoutDate() != null) {
            addWarningBox(document, "Projected cash-out date: " + forecast.projectedCashoutDate());
        }
        document.add(Chunk.NEWLINE);
    }

    private void addRecommendations(Document document, List<CashFlowDto.Response> history, ForecastDto.Response forecast) throws DocumentException {
        com.lowagie.text.List bullets = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
        bullets.setIndentationLeft(14f);
        if (forecast != null && forecast.alerts() != null) { for (String alert : forecast.alerts()) { bullets.add(new ListItem(alert, font(10, Font.NORMAL, BRAND_DARK))); } }
        if (forecast != null && forecast.explanations() != null) { for (String exp : forecast.explanations()) { bullets.add(new ListItem(exp, font(10, Font.NORMAL, BRAND_DARK))); } }
        BigDecimal latestBalance = history.isEmpty() ? BigDecimal.ZERO : nz(history.get(history.size() - 1).cumulativeBalance());
        if (latestBalance.signum() < 0) { bullets.add(new ListItem("Immediate action recommended: current cumulative balance is negative.", font(10, Font.NORMAL, BRAND_DARK))); }
        if (forecast != null && forecast.avgPredictedNetFlow() != null && forecast.avgPredictedNetFlow().signum() < 0) { bullets.add(new ListItem("The average predicted net flow is negative. Review expenses.", font(10, Font.NORMAL, BRAND_DARK))); }
        else { bullets.add(new ListItem("Use the positive forecast window to build a treasury buffer.", font(10, Font.NORMAL, BRAND_DARK))); }
        document.add(bullets);
        document.add(Chunk.NEWLINE);
    }

    private void addMethodology(Document document, ForecastDto.Response forecast) throws DocumentException {
        Paragraph p = new Paragraph(
                "This report is automatically generated from historical cash flow summaries and forecast outputs available in HELMA. "
                        + "Forecast values are indicative and depend on the quality of historical data (" + safe(forecast != null ? forecast.forecastMethod() : null, "NO_DATA") + "). "
                        + "It should be used as a decision-support document, not as a guaranteed financial outcome.",
                font(10, Font.NORMAL, BRAND_DARK));
        p.setLeading(16f);
        document.add(p);
    }

    /* ── Shared formatting ──────────────────────────────────────────── */

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, font(15, Font.BOLD, BRAND_PURPLE));
        p.setSpacingBefore(10f);
        p.setSpacingAfter(10f);
        document.add(p);
    }

    private PdfPCell kpiCell(String label, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12f);
        cell.setBorderColor(new Color(220, 220, 230));
        cell.setBackgroundColor(Color.WHITE);
        cell.addElement(new Paragraph(label, font(9, Font.NORMAL, new Color(100, 100, 120))));
        Paragraph v = new Paragraph(value, font(13, Font.BOLD, valueColor));
        v.setSpacingBefore(6f);
        cell.addElement(v);
        return cell;
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(8f);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) { addCell(table, text, BRAND_DARK); }

    private void addCell(PdfPTable table, String text, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.NORMAL, color)));
        cell.setPadding(7f);
        cell.setBorderColor(new Color(225, 225, 235));
        table.addCell(cell);
    }

    private Paragraph line(String label, String value) {
        Paragraph p = new Paragraph();
        p.setLeading(18f);
        p.add(new Chunk(label + ": ", font(11, Font.BOLD, BRAND_PURPLE)));
        p.add(new Chunk(value, font(11, Font.NORMAL, BRAND_DARK)));
        return p;
    }

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private String money(BigDecimal value) {
        return nz(value).setScale(0, RoundingMode.HALF_UP) + " TND";
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildOutlookText(List<CashFlowDto.Response> history, ForecastDto.Response forecast) {
        BigDecimal latestBalance = history.isEmpty() ? BigDecimal.ZERO : nz(history.get(history.size() - 1).cumulativeBalance());
        if (latestBalance.signum() < 0) return "Current liquidity is under pressure — latest cumulative balance is negative.";
        if (forecast != null && forecast.projectedCashoutDate() != null) return "Forecast indicates a cash-out risk around " + forecast.projectedCashoutDate() + ".";
        if (forecast != null && nz(forecast.avgPredictedNetFlow()).signum() >= 0) return "Forecast suggests a stable or positive short-term cash position.";
        return "Forecast shows pressure on operating cash — tighten expense control.";
    }

    /* ── Header/Footer ──────────────────────────────────────────────── */

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String userName;
        private final String reportType;

        private HeaderFooterEvent(String userName, String reportType) {
            this.userName = userName;
            this.reportType = reportType;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.setColorStroke(new Color(220, 220, 230));
            cb.moveTo(document.left(), document.top() + 12);
            cb.lineTo(document.right(), document.top() + 12);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("HELMA · " + reportType + " · " + userName,
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(110, 110, 130))),
                    document.left(), document.top() + 18, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(110, 110, 130))),
                    document.right(), document.bottom() - 18, 0);
            cb.restoreState();
        }
    }
}
