package com.helma.helmabackend.controller;


import com.helma.helmabackend.entity.SavingsDeposit;
import com.helma.helmabackend.entity.SavingsGoal;
import com.helma.helmabackend.repository.SavingsDepositRepository;
import com.helma.helmabackend.repository.SavingsGoalRepository;
import com.helma.helmabackend.repository.VoucherRepository;
import com.helma.helmabackend.service.AiRecommendationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:4200")
public class StatsController {

    private final SavingsGoalRepository goalRepository;
    private final SavingsDepositRepository depositRepository;
    private final VoucherRepository voucherRepository;
    private final AiRecommendationService aiService;

    public StatsController(
            SavingsGoalRepository goalRepository,
            SavingsDepositRepository depositRepository,
            VoucherRepository voucherRepository,
            AiRecommendationService aiService
    ) {
        this.goalRepository = goalRepository;
        this.depositRepository = depositRepository;
        this.voucherRepository = voucherRepository;
        this.aiService = aiService;
    }

    /* =====================================================
       USER DASHBOARD
    ===================================================== */
    @GetMapping("/dashboard/user/{userId}")
    public Map<String, Object> userDashboard(
            @PathVariable Long userId
    ) {

        List<SavingsGoal> goals =
                goalRepository.findByUserId(userId);

        List<SavingsDeposit> deposits =
                depositRepository.findBySavingsGoalUserId(userId);

        return buildDashboard(goals, deposits, userId);
    }

    /* =====================================================
       ADMIN DASHBOARD
    ===================================================== */
    @GetMapping("/dashboard/admin")
    public Map<String, Object> adminDashboard() {

        List<SavingsGoal> goals =
                goalRepository.findAll();

        List<SavingsDeposit> deposits =
                depositRepository.findAll();

        return buildDashboard(goals, deposits, null);
    }

    /* =====================================================
       ADMIN AI SUMMARY
       /api/stats/dashboard/admin/summary?size=short
       short | medium | long
    ===================================================== */
    @GetMapping("/dashboard/admin/summary")
    public String adminSummary(
            @RequestParam(defaultValue = "medium")
            String size
    ) {

        List<SavingsGoal> goals =
                goalRepository.findAll();

        List<SavingsDeposit> deposits =
                depositRepository.findAll();

        return buildAdminSummary(
                goals,
                deposits,
                size
        );
    }

    /* =====================================================
       COMMON DASHBOARD BUILDER
    ===================================================== */
    private Map<String, Object> buildDashboard(
            List<SavingsGoal> goals,
            List<SavingsDeposit> deposits,
            Long userId
    ) {

        Map<String, Object> res =
                new HashMap<>();

        /* KPI */

        double totalSaved =
                goals.stream()
                        .mapToDouble(
                                SavingsGoal::getCurrentAmount
                        )
                        .sum();

        double totalTarget =
                goals.stream()
                        .mapToDouble(
                                SavingsGoal::getTargetAmount
                        )
                        .sum();

        res.put("totalSaved", totalSaved);
        res.put("totalTarget", totalTarget);
        res.put("goalCount", goals.size());
        res.put("depositCount", deposits.size());

        double progress =
                totalTarget == 0 ? 0 :
                        (totalSaved * 100)
                                / totalTarget;

        res.put(
                "progress",
                Math.round(progress)
        );

        /* USERS COUNT */

        long users =
                goals.stream()
                        .map(g -> g.getUser().getId())
                        .distinct()
                        .count();

        res.put("userCount", users);

        /* MONTHLY CHART */

        Map<String, Double> byMonth =
                new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {

            YearMonth ym =
                    YearMonth.now()
                            .minusMonths(i);

            double amount =
                    deposits.stream()
                            .filter(d ->
                                    YearMonth.from(
                                            d.getDateDeposit()
                                    ).equals(ym)
                            )
                            .mapToDouble(
                                    SavingsDeposit::getAmount
                            )
                            .sum();

            byMonth.put(
                    ym.getMonth()
                            .name()
                            .substring(0, 3),
                    amount
            );
        }

        res.put("depositsByMonth", byMonth);

        /* DAILY */

        Map<String, Double> byDay =
                new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {

            LocalDate day =
                    LocalDate.now()
                            .minusDays(i);

            double amount =
                    deposits.stream()
                            .filter(d ->
                                    d.getDateDeposit()
                                            .equals(day)
                            )
                            .mapToDouble(
                                    SavingsDeposit::getAmount
                            )
                            .sum();

            byDay.put(
                    day.getDayOfWeek()
                            .name()
                            .substring(0, 3),
                    amount
            );
        }

        res.put("depositsByDay", byDay);

        /* STATUS */

        Map<String, Integer> status =
                new LinkedHashMap<>();

        status.put(
                "ACHIEVED",
                (int) goals.stream()
                        .filter(g ->
                                g.getStatus()
                                        .name()
                                        .equals("ACHIEVED")
                        ).count()
        );

        status.put(
                "IN_PROGRESS",
                (int) goals.stream()
                        .filter(g ->
                                g.getStatus()
                                        .name()
                                        .equals("IN_PROGRESS")
                        ).count()
        );

        status.put(
                "EXPIRED",
                (int) goals.stream()
                        .filter(g ->
                                g.getStatus()
                                        .name()
                                        .equals("EXPIRED")
                        ).count()
        );

        res.put("goalStatus", status);

        /* TOP GOALS */

        List<Map<String, Object>> top =
                new ArrayList<>();

        goals.stream()
                .sorted((a, b) ->
                        Double.compare(
                                b.getCurrentAmount(),
                                a.getCurrentAmount()
                        ))
                .limit(5)
                .forEach(g -> {

                    Map<String, Object> item =
                            new HashMap<>();

                    double p =
                            g.getTargetAmount() == 0
                                    ? 0
                                    : (g.getCurrentAmount() * 100)
                                    / g.getTargetAmount();

                    item.put("title", g.getTitle());
                    item.put("saved", g.getCurrentAmount());
                    item.put("target", g.getTargetAmount());
                    item.put("percent", Math.round(p));

                    top.add(item);
                });

        res.put("topGoals", top);

        /* VOUCHERS */

        long totalVouchers;

        if (userId == null) {
            totalVouchers =
                    voucherRepository.findAll()
                            .size();
        } else {
            totalVouchers =
                    voucherRepository
                            .findBySavingsGoalUserId(userId)
                            .size();
        }

        res.put(
                "totalVouchers",
                totalVouchers
        );

        /* RECOMMENDATION */

        res.put(
                "recommendation",
                buildRecommendation(
                        goals,
                        deposits
                )
        );

        return res;
    }

    /* =====================================================
       USER / ADMIN SMALL AI
    ===================================================== */
    private String buildRecommendation(
            List<SavingsGoal> goals,
            List<SavingsDeposit> deposits
    ) {

        if (goals.isEmpty()) {
            return "No goals found. Create goals to start analytics.";
        }

        double avg =
                deposits.stream()
                        .mapToDouble(
                                SavingsDeposit::getAmount
                        )
                        .average()
                        .orElse(0);

        long achieved =
                goals.stream()
                        .filter(g ->
                                g.getStatus()
                                        .name()
                                        .equals("ACHIEVED")
                        )
                        .count();

        if (achieved >= 3) {
            return "Strong performance detected. Savings ecosystem is healthy.";
        }

        if (avg < 100) {
            return "Deposit average is low. Encourage stronger recurring deposits.";
        }

        return "Stable platform growth with positive user momentum.";
    }

    /* =====================================================
       ADMIN LONG SUMMARY
    ===================================================== */
    private String buildAdminSummary(
            List<SavingsGoal> goals,
            List<SavingsDeposit> deposits,
            String size
    ) {

        double saved =
                goals.stream()
                        .mapToDouble(
                                SavingsGoal::getCurrentAmount
                        )
                        .sum();

        double target =
                goals.stream()
                        .mapToDouble(
                                SavingsGoal::getTargetAmount
                        )
                        .sum();

        long users =
                goals.stream()
                        .map(g -> g.getUser().getId())
                        .distinct()
                        .count();

        long achieved =
                goals.stream()
                        .filter(g ->
                                g.getStatus()
                                        .name()
                                        .equals("ACHIEVED")
                        )
                        .count();

        double progress =
                target == 0 ? 0 :
                        (saved * 100) / target;

        String shortTxt =
                "Platform has "
                        + users + " active users, "
                        + goals.size() + " goals and "
                        + deposits.size()
                        + " deposits. Global progress is "
                        + Math.round(progress) + "%.";

        String mediumTxt =
                shortTxt +
                        " Achieved goals: "
                        + achieved +
                        ". Total saved capital: "
                        + Math.round(saved)
                        + " TND. Recommend increasing user engagement campaigns and reward incentives.";

        String longTxt =
                mediumTxt +
                        " Deposit behavior indicates current platform traction with opportunities for growth. Focus on retention, goal completion journeys, gamified rewards and automated reminders. Consider segmenting users by performance tiers and launching targeted savings challenges to increase recurring deposits.";

        switch (size.toLowerCase()) {

            case "short":
                return shortTxt;

            case "long":
                return longTxt;

            default:
                return mediumTxt;
        }
    }
}