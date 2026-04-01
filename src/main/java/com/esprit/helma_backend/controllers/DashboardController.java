package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.DashboardDto;
import com.esprit.helma_backend.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<DashboardDto.Response> getDashboard(@PathVariable Long userId) {
        return ResponseEntity.ok(dashboardService.getDashboard(userId));
    }
}