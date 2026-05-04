package com.helma.helmabackend.controller;


import com.helma.helmabackend.entity.SavingsDeposit;
import com.helma.helmabackend.entity.SavingsGoal;
import com.helma.helmabackend.service.AiRecommendationService;
import com.helma.helmabackend.service.SavingsDepositService;
import com.helma.helmabackend.service.SavingsGoalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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