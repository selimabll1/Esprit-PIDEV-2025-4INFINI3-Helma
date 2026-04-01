package esprit.tn.projet_pi.controller;

import esprit.tn.projet_pi.entity.SavingsDeposit;
import esprit.tn.projet_pi.entity.SavingsGoal;
import esprit.tn.projet_pi.service.AiRecommendationService;
import esprit.tn.projet_pi.service.SavingsDepositService;
import esprit.tn.projet_pi.service.SavingsGoalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {

    private final AiRecommendationService aiService;
    private final SavingsGoalService goalService;
    private final SavingsDepositService depositService;

    public AiRecommendationController(AiRecommendationService aiService,
                                      SavingsGoalService goalService,
                                      SavingsDepositService depositService) {
        this.aiService = aiService;
        this.goalService = goalService;
        this.depositService = depositService;
    }

    @GetMapping("/advice/{goalId}")
    public String getAdvice(@PathVariable Long goalId){

        SavingsGoal goal = goalService.getGoalById(goalId);
        List<SavingsDeposit> deposits = depositService.getDepositsByGoal(goalId);

        return aiService.generateAdvice(goal, deposits);
    }
}