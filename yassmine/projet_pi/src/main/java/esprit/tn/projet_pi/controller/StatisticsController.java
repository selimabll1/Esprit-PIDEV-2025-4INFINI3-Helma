package esprit.tn.projet_pi.controller;

import esprit.tn.projet_pi.dto.DepositStatsDTO;
import esprit.tn.projet_pi.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import esprit.tn.projet_pi.dto.GoalPredictionDTO;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/deposits")
    public ResponseEntity<DepositStatsDTO> getGlobalDepositStats() {
        return ResponseEntity.ok(statisticsService.getGlobalDepositStats());
    }

    @GetMapping("/deposits/goal/{goalId}")
    public ResponseEntity<DepositStatsDTO> getDepositStatsByGoal(@PathVariable Long goalId) {
        return ResponseEntity.ok(statisticsService.getDepositStatsByGoal(goalId));
    }
    //*prediction
    @GetMapping("/prediction/{goalId}")
    public GoalPredictionDTO predictGoal(@PathVariable Long goalId) {
        return statisticsService.predictGoalAchievement(goalId);
    }
}