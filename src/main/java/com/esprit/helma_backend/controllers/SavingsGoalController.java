package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.SavingsGoalDto;
import com.esprit.helma_backend.services.SavingsGoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
@CrossOrigin(origins = "*")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDto.Response> create(@RequestBody SavingsGoalDto.Create req) {
        return ResponseEntity.ok(savingsGoalService.create(req));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SavingsGoalDto.Response>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(savingsGoalService.getByUser(userId));
    }

    @PatchMapping("/{goalId}/progress")
    public ResponseEntity<SavingsGoalDto.Response> addProgress(@PathVariable Long goalId,
                                                               @RequestBody SavingsGoalDto.AddProgress req) {
        return ResponseEntity.ok(savingsGoalService.addProgress(goalId, req.amount()));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> delete(@PathVariable Long goalId) {
        savingsGoalService.delete(goalId);
        return ResponseEntity.noContent().build();
    }
}