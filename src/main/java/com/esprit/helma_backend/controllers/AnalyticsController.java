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

    @GetMapping("/month-comparison")
    public ResponseEntity<Map<String, Object>> getMonthComparison(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {

        ZoneId zone = ZoneId.systemDefault();
        LocalDate currentStart  = month.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);

        Instant curFrom  = currentStart.atStartOfDay(zone).toInstant();
        Instant curTo    = currentStart.plusMonths(1).atStartOfDay(zone).toInstant();
        Instant prevFrom = previousStart.atStartOfDay(zone).toInstant();
        Instant prevTo   = currentStart.atStartOfDay(zone).toInstant();

        BigDecimal curIncome  = nz(txRepo.sumIncomeForUserBetween(userId, curFrom, curTo));
        BigDecimal curExpense = nz(txRepo.sumExpenseForUserBetween(userId, curFrom, curTo));
        BigDecimal curNet     = curIncome.subtract(curExpense);

        BigDecimal prevIncome  = nz(txRepo.sumIncomeForUserBetween(userId, prevFrom, prevTo));
        BigDecimal prevExpense = nz(txRepo.sumExpenseForUserBetween(userId, prevFrom, prevTo));
        BigDecimal prevNet     = prevIncome.subtract(prevExpense);

        Map<String, Object> curTopCat  = getTopCategory(userId, curFrom, curTo);
        Map<String, Object> prevTopCat = getTopCategory(userId, prevFrom, prevTo);

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("totalIncome",       curIncome.setScale(2, RoundingMode.HALF_UP));
        current.put("totalExpense",      curExpense.setScale(2, RoundingMode.HALF_UP));
        current.put("netFlow",           curNet.setScale(2, RoundingMode.HALF_UP));
        current.put("topCategory",       curTopCat.get("category"));
        current.put("topCategoryAmount", curTopCat.get("amount"));

        Map<String, Object> previous = new LinkedHashMap<>();
        previous.put("totalIncome",       prevIncome.setScale(2, RoundingMode.HALF_UP));
        previous.put("totalExpense",      prevExpense.setScale(2, RoundingMode.HALF_UP));
        previous.put("netFlow",           prevNet.setScale(2, RoundingMode.HALF_UP));
        previous.put("topCategory",       prevTopCat.get("category"));
        previous.put("topCategoryAmount", prevTopCat.get("amount"));

        boolean hasPrevData = prevIncome.compareTo(BigDecimal.ZERO) > 0
                || prevExpense.compareTo(BigDecimal.ZERO) > 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentMonth",  current);
        result.put("previousMonth", previous);

        if (!hasPrevData) {
            result.put("changes", null);
            result.put("insight", null);
            return ResponseEntity.ok(result);
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("incomeChange",      pctChange(curIncome, prevIncome));
        changes.put("expenseChange",     pctChange(curExpense, prevExpense));
        changes.put("netFlowChange",     pctChange(curNet, prevNet));
        changes.put("topCategoryChange", pctChange(
                (BigDecimal) curTopCat.getOrDefault("amount", BigDecimal.ZERO),
                (BigDecimal) prevTopCat.getOrDefault("amount", BigDecimal.ZERO)));
        result.put("changes", changes);

        result.put("insight", generateInsight(
                (BigDecimal) changes.get("incomeChange"),
                (BigDecimal) changes.get("expenseChange"),
                curTopCat, (BigDecimal) changes.get("topCategoryChange")));

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> getTopCategory(Long userId, Instant from, Instant to) {
        var grouped = txRepo.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .filter(t -> "EXPENSE".equals(t.getType().name()))
                .filter(t -> t.getTxnDate() != null && !t.getTxnDate().isBefore(from) && t.getTxnDate().isBefore(to))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Other",
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        Map<String, Object> out = new LinkedHashMap<>();
        if (grouped.isEmpty()) { out.put("category", null); out.put("amount", BigDecimal.ZERO); return out; }
        var top = grouped.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        out.put("category", top.getKey());
        out.put("amount", top.getValue().setScale(2, RoundingMode.HALF_UP));
        return out;
    }

    private BigDecimal pctChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private String generateInsight(BigDecimal incomeChange, BigDecimal expenseChange,
                                   Map<String, Object> curTopCat, BigDecimal topCatChange) {
        String topCatName = curTopCat.get("category") instanceof String s ? s : "Miscellaneous";

        if (topCatChange != null && Math.abs(topCatChange.doubleValue()) >= 20) {
            String dir = topCatChange.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
            return "Your " + topCatName + " spending " + dir + " "
                    + Math.abs(topCatChange.longValue()) + "% compared to last month";
        }
        if (expenseChange != null && Math.abs(expenseChange.doubleValue()) >= 10) {
            String dir = expenseChange.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
            return "Your total spending " + dir + " "
                    + Math.abs(expenseChange.longValue()) + "% compared to last month";
        }
        if (incomeChange != null && Math.abs(incomeChange.doubleValue()) >= 10) {
            String dir = incomeChange.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
            return "Your income " + dir + " "
                    + Math.abs(incomeChange.longValue()) + "% compared to last month";
        }
        return "Your spending patterns are consistent with last month";
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
        BigDecimal totalBudgetLimit = budgets.stream()
                .map(b -> b.getLimitAmount() != null ? b.getLimitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = txRepo.sumExpenseForUserBetween(userId, from, to);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        BigDecimal totalMonthlyIncome = txRepo.sumIncomeForUserBetween(userId, from, to);
        if (totalMonthlyIncome == null) totalMonthlyIncome = BigDecimal.ZERO;

        BigDecimal remaining;
        String basis;

        if (!budgets.isEmpty()) {
            // Budget-based: user has configured category limits for this month
            remaining = totalBudgetLimit.subtract(totalSpent);
            basis = "BUDGET";
        } else {
            // Income-based fallback: no budgets configured, use income vs spending
            remaining = totalMonthlyIncome.subtract(totalSpent);
            basis = "INCOME";
        }

        // dailyAllowance is 0 when over limit, but we preserve the real (negative) remaining
        BigDecimal dailyAllowance = remaining.compareTo(BigDecimal.ZERO) > 0
                ? remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("basis", basis);
        result.put("totalBudget", totalBudgetLimit.setScale(2, RoundingMode.HALF_UP));
        result.put("totalMonthlyIncome", totalMonthlyIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP));
        result.put("remaining", remaining.setScale(2, RoundingMode.HALF_UP));
        result.put("daysLeft", daysLeft);
        result.put("dailyAllowance", dailyAllowance);
        return ResponseEntity.ok(result);
    }
}
