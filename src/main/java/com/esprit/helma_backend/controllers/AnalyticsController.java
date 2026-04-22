package com.esprit.helma_backend.controllers;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.BudgetRepository;
import com.esprit.helma_backend.entities.Budget;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final TransactionRepository txRepo;
    private final BudgetRepository budgetRepo;

    public AnalyticsController(TransactionRepository txRepo, BudgetRepository budgetRepo) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<Map<String, Object>>> getByCategory(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        LocalDate monthStart = month.withDayOfMonth(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        var txs = txRepo.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .filter(t -> "EXPENSE".equals(t.getType().name()))
                .filter(t -> t.getTxnDate() != null && !t.getTxnDate().isBefore(from) && t.getTxnDate().isBefore(to))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Other",
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add)));

        List<Map<String, Object>> result = new ArrayList<>();
        txs.forEach((cat, total) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat);
            m.put("amount", total.setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        });
        result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<Map<String, Object>>> getDaily(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        LocalDate monthStart = month.withDayOfMonth(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        var txs = txRepo.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .filter(t -> "EXPENSE".equals(t.getType().name()))
                .filter(t -> t.getTxnDate() != null && !t.getTxnDate().isBefore(from) && t.getTxnDate().isBefore(to))
                .collect(Collectors.groupingBy(
                        t -> t.getTxnDate().atZone(zone).toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add)));

        List<Map<String, Object>> result = new ArrayList<>();
        txs.forEach((day, total) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day);
            m.put("amount", total.setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        });
        result.sort(Comparator.comparing(a -> (String) a.get("date")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/budget-vs-actual")
    public ResponseEntity<List<Map<String, Object>>> getBudgetVsActual(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        LocalDate monthStart = month.withDayOfMonth(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        List<Budget> budgets = budgetRepo.findByUserIdAndMonthStartOrderByCategoryAsc(userId, monthStart);

        var spending = txRepo.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .filter(t -> "EXPENSE".equals(t.getType().name()))
                .filter(t -> t.getTxnDate() != null && !t.getTxnDate().isBefore(from) && t.getTxnDate().isBefore(to))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory().toLowerCase() : "other",
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Budget b : budgets) {
            String cat = b.getCategory() != null ? b.getCategory() : "General";
            BigDecimal limit = b.getLimitAmount() != null ? b.getLimitAmount() : BigDecimal.ZERO;
            BigDecimal spent = spending.getOrDefault(cat.toLowerCase(), BigDecimal.ZERO);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", cat);
            m.put("budgetLimit", limit.setScale(2, RoundingMode.HALF_UP));
            m.put("actualSpent", spent.setScale(2, RoundingMode.HALF_UP));
            m.put("remaining", limit.subtract(spent).setScale(2, RoundingMode.HALF_UP));
            m.put("usagePercent", limit.compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(limit, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-allowance")
    public ResponseEntity<Map<String, Object>> getDailyAllowance(@RequestParam Long userId) {

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);
        int daysLeft = Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(today, nextMonth));

        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = nextMonth.atStartOfDay(zone).toInstant();

        List<Budget> budgets = budgetRepo.findByUserIdAndMonthStartOrderByCategoryAsc(userId, monthStart);
        BigDecimal totalBudget = budgets.stream()
                .map(b -> b.getLimitAmount() != null ? b.getLimitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = txRepo.sumExpenseForUserBetween(userId, from, to);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        BigDecimal remaining = totalBudget.subtract(totalSpent);
        BigDecimal dailyAllowance = remaining.compareTo(BigDecimal.ZERO) > 0
                ? remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("totalBudget", totalBudget.setScale(2, RoundingMode.HALF_UP));
        result.put("totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP));
        result.put("remaining", remaining.setScale(2, RoundingMode.HALF_UP));
        result.put("daysLeft", daysLeft);
        result.put("dailyAllowance", dailyAllowance);
        return ResponseEntity.ok(result);
    }
}
