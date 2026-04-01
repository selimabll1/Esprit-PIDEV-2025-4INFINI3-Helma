package com.esprit.helma_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Step 3 — Burn Rate & Runway DTOs
 *
 * Burn rate  = average monthly EXPENSE over the last 3 complete months
 * Runway     = current cumulative balance / burn_rate  (in months)
 * Status     = HEALTHY | WARNING | CRITICAL
 */
public class BurnRateDto {

    // ── Request ─────────────────────────────────────────────────────────────

    // No request body needed — userId comes from path variable

    // ── Response ────────────────────────────────────────────────────────────

    /**
     * Full burn-rate snapshot returned by GET /api/burn-rate/{userId}
     */
    public record Response(

            Long userId,

            // Core metrics
            BigDecimal burnRate,           // avg monthly expenses (last 3 months), TND
            BigDecimal currentBalance,     // latest cumulative_balance from cash_flow_summaries
            BigDecimal runwayMonths,       // currentBalance / burnRate  (null if burnRate = 0)

            // Status
            RunwayStatus status,           // HEALTHY | WARNING | CRITICAL
            boolean riskCaseTriggered,     // true if a RiskCase was auto-created/updated

            // Computation metadata
            String burnRateMethod,         // "3M_AVG" | "1M_AVG" | "2M_AVG" | "NO_DATA"
            int monthsUsed,                // how many months of data were available (0-3)

            // Month breakdown used for the average
            List<MonthExpense> breakdown,  // per-month expenses used in the average

            // FinCoach AI tips
            List<String> finCoachTips,     // 3-4 actionable tips (Groq or rule-based fallback)

            // Computed dates
            LocalDate computedAt,          // today
            LocalDate projectedZeroDate    // computedAt + runwayMonths (null if no runway)

    ) {}

    /**
     * Per-month expense detail shown in the breakdown array
     */
    public record MonthExpense(
            LocalDate monthStart,
            BigDecimal totalExpense
    ) {}

    /**
     * Runway health status thresholds:
     *   CRITICAL : runway < 2 months  → triggers RiskCase
     *   WARNING  : runway < 4 months  → no RiskCase but flagged
     *   HEALTHY  : runway >= 4 months
     */
    public enum RunwayStatus {
        HEALTHY,
        WARNING,
        CRITICAL
    }
}