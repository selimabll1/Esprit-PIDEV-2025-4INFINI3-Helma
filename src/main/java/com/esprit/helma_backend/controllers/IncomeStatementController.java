package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.IncomeStatementDto;
import com.esprit.helma_backend.services.IncomeStatementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/income-statement")
@CrossOrigin(origins = "*")
public class IncomeStatementController {

    private final IncomeStatementService incomeStatementService;

    public IncomeStatementController(IncomeStatementService incomeStatementService) {
        this.incomeStatementService = incomeStatementService;
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<IncomeStatementDto.Response>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(incomeStatementService.getHistory(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<IncomeStatementDto.Response> getCurrent(@PathVariable Long userId) {
        return ResponseEntity.ok(incomeStatementService.getCurrent(userId));
    }
}