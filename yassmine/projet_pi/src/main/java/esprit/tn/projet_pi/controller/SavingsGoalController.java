package esprit.tn.projet_pi.controller;


import esprit.tn.projet_pi.entity.SavingsGoal;
import esprit.tn.projet_pi.service.SavingsGoalService;
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

    // UPDATE
    @PutMapping("/{id}")
    public SavingsGoal update(@PathVariable Long id, @RequestBody SavingsGoal goal){
        return service.updateGoal(id, goal);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.deleteGoal(id);
    }
}
