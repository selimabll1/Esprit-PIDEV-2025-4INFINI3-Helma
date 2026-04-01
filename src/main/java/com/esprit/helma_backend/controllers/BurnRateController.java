package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.BurnRateDto;
import com.esprit.helma_backend.services.BurnRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Step 3 — Burn Rate & Runway REST Controller
 *
 * Endpoints:
 *   GET  /api/burn-rate/{userId}          → latest computed snapshot
 *   POST /api/burn-rate/{userId}/refresh  → force recompute (for testing)
 */
@RestController
@RequestMapping("/api/burn-rate")
public class BurnRateController {

    private final BurnRateService burnRateService;

    public BurnRateController(BurnRateService burnRateService) {
        this.burnRateService = burnRateService;
    }

    /**
     * GET /api/burn-rate/{userId}
     *
     * Returns the full burn-rate snapshot for the user:
     * burn_rate, current_balance, runway_months, status, fin_coach_tips.
     *
     * Always recomputes on the fly (uses cached CashFlowSummary rows,
     * so it's fast — just 3 JPA queries).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<BurnRateDto.Response> getBurnRate(@PathVariable Long userId) {
        return ResponseEntity.ok(burnRateService.compute(userId));
    }

    /**
     * POST /api/burn-rate/{userId}/refresh
     *
     * Force-recomputes the burn rate (same logic as GET,
     * exposed separately for Swagger clarity and testing).
     * Useful after bulk-importing transactions.
     */
    @PostMapping("/{userId}/refresh")
    public ResponseEntity<BurnRateDto.Response> refresh(@PathVariable Long userId) {
        return ResponseEntity.ok(burnRateService.compute(userId));
    }
}