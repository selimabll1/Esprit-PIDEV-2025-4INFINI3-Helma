package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.CashFlowDto;
import com.esprit.helma_backend.services.CashFlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cashflow")
public class CashFlowController {

    private final CashFlowService cashFlowService;

    public CashFlowController(CashFlowService cashFlowService) {
        this.cashFlowService = cashFlowService;
    }

    // GET current month summary
    @GetMapping("/{userId}")
    public ResponseEntity<CashFlowDto.Response> getCurrentMonth(@PathVariable Long userId) {
        return ResponseEntity.ok(cashFlowService.getCurrentMonth(userId));
    }

    // GET full history (all months)
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<CashFlowDto.Response>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(cashFlowService.getHistory(userId));
    }
}