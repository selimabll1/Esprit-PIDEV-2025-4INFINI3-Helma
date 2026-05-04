package com.helma.helmabackend.controller;



import com.helma.helmabackend.entity.SavingsGoal;
import com.helma.helmabackend.service.SavingsGoalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService service;

    public SavingsGoalController(SavingsGoalService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public SavingsGoal create(@RequestBody SavingsGoal goal){
        return service.createGoal(goal);
    }
    // GET ONE GOAL
    @GetMapping("/{id}")
    public SavingsGoal getOne(@PathVariable Long id){
        return service.getGoalById(id);
    }
    // READ
    @GetMapping
    public List<SavingsGoal> getAll(){
        return service.getUserGoals();
    }
    @GetMapping("/admin")
    public List<SavingsGoal> getAllGoals(){
        return service.getAllGoals();
    }
    // UPDATE
    @PutMapping("/{id}")
    public SavingsGoal update(@PathVariable Long id, @RequestBody SavingsGoal goal){
        return service.updateGoal(id, goal);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.deleteGoal(id);
    }
}
