package com.helma.helmabackend.controller;

import com.helma.helmabackend.dto.savings.RiskAnalysisDTO;
import com.helma.helmabackend.service.SavingsGoalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Administrative Savings Goal operations.
 * Handles specialized risk analysis and monitoring for platform administrators.
 */
@RestController
@RequestMapping("/api/admin/goals")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminSavingsGoalController {

    private final SavingsGoalService service;

    public AdminSavingsGoalController(SavingsGoalService service) {
        this.service = service;
    }

    /**
     * Retrieves a full risk analysis report for all savings goals.
     * @return List of RiskAnalysisDTO containing status and recommendations
     */
    @GetMapping("/risk-analysis")
    public List<RiskAnalysisDTO> getRiskAnalysis() {
        return service.getRiskAnalysis();
    }
}
