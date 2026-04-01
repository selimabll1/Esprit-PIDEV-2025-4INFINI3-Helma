package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.BudgetDto;
import com.esprit.helma_backend.services.BudgetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetDto.Response> create(@RequestBody BudgetDto.Create req) {
        return ResponseEntity.ok(budgetService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<BudgetDto.Response>> getByUser(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate monthStart
    ) {
        if (userId == null) {
            return ResponseEntity.ok(budgetService.getAll());
        }
        if (monthStart != null) {
            return ResponseEntity.ok(budgetService.getByUserAndMonth(userId, monthStart));
        }
        return ResponseEntity.ok(budgetService.getByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto.Response> update(@PathVariable Long id, @RequestBody BudgetDto.Update req) {
        return ResponseEntity.ok(budgetService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trust-budget/{userId}")
    public ResponseEntity<BudgetDto.TrustBudgetResponse> getTrustBudget(@PathVariable Long userId) {
        return ResponseEntity.ok(budgetService.getTrustBudget(userId));
    }
}