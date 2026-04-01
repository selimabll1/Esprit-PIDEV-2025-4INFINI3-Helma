package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.HealthScoreDto;
import com.esprit.helma_backend.services.FinancialHealthScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health-score")
public class HealthScoreController {

    private final FinancialHealthScoreService healthScoreService;

    public HealthScoreController(FinancialHealthScoreService healthScoreService) {
        this.healthScoreService = healthScoreService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<HealthScoreDto.Response> get(@PathVariable Long userId) {
        return ResponseEntity.ok(healthScoreService.compute(userId));
    }
}