package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.ForecastDto;
import com.esprit.helma_backend.services.CashFlowForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final CashFlowForecastService forecastService;

    public ForecastController(CashFlowForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ForecastDto.Response> get(@PathVariable Long userId) {
        return ResponseEntity.ok(forecastService.forecast(userId));
    }
}