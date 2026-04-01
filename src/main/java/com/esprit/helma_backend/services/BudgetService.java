package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.BudgetDto;
import com.esprit.helma_backend.dto.TrustBadgeDto;
import com.esprit.helma_backend.entities.Budget;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.BudgetRepository;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepo;
    private final UserRepository userRepo;
    private final TransactionRepository txRepo;
    private final RiskCaseRepository riskRepo;
    private final TrustBadgeService badgeService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal BD_100 = new BigDecimal("100");
    private static final BigDecimal BD_1_5 = new BigDecimal("1.5");
    private static final BigDecimal BD_0_6 = new BigDecimal("0.6");
    private static final BigDecimal BD_0_4 = new BigDecimal("0.4");
    private static final BigDecimal BD_0_8 = new BigDecimal("0.8");
    private static final BigDecimal BD_200 = new BigDecimal("200");
    private static final BigDecimal BD_2 = new BigDecimal("2");

    public BudgetService(BudgetRepository budgetRepo,
                         UserRepository userRepo,
                         TransactionRepository txRepo,
                         RiskCaseRepository riskRepo,
                         TrustBadgeService badgeService) {
        this.budgetRepo = budgetRepo;
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.riskRepo = riskRepo;
        this.badgeService = badgeService;
    }

    private static BudgetDto.Response toResponse(Budget b) {
        return new BudgetDto.Response(
                b.getId(),
                b.getUserId(),
                b.getLimitAmount(),
                b.getMonthStart(),
                b.getCategory()
        );
    }

    public BudgetDto.Response create(BudgetDto.Create req) {
        userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedCategory = normalizeCategory(req.category());

        Budget b = budgetRepo.findByUserIdAndMonthStartAndCategory(
                        req.userId(),
                        req.monthStart(),
                        normalizedCategory
                )
                .orElseGet(Budget::new);

        b.setUserId(req.userId());
        b.setLimitAmount(req.limitAmount());
        b.setMonthStart(req.monthStart());
        b.setCategory(normalizedCategory);

        return toResponse(budgetRepo.save(b));
    }

    @Transactional(readOnly = true)
    public BudgetDto.Response getById(Long id) {
        Budget b = budgetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public List<BudgetDto.Response> getAll() {
        return budgetRepo.findAll().stream().map(BudgetService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetDto.Response> getByUser(Long userId) {
        userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return budgetRepo.findByUserIdOrderByMonthStartDesc(userId)
                .stream()
                .map(BudgetService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetDto.Response> getByUserAndMonth(Long userId, LocalDate monthStart) {
        userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return budgetRepo.findByUserIdAndMonthStartOrderByCategoryAsc(userId, monthStart)
                .stream()
                .map(BudgetService::toResponse)
                .toList();
    }

    public BudgetDto.Response update(Long id, BudgetDto.Update req) {
        Budget b = budgetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));

        b.setLimitAmount(req.limitAmount());
        b.setMonthStart(req.monthStart());
        b.setCategory(normalizeCategory(req.category()));

        return toResponse(budgetRepo.save(b));
    }

    public void delete(Long id) {
        if (!budgetRepo.existsById(id)) {
            throw new IllegalArgumentException("Budget not found");
        }
        budgetRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public BudgetDto.TrustBudgetResponse getTrustBudget(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isEntrepreneur = Boolean.TRUE.equals(user.getIsEntrepreneur());
        if (!isEntrepreneur) {
            throw new IllegalArgumentException("Entrepreneur-only feature");
        }

        List<String> reasons = new ArrayList<>();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = currentMonthStart.plusMonths(1);

        Instant currentMonthStartI = currentMonthStart.atStartOfDay(zone).toInstant();
        Instant nextMonthStartI = nextMonthStart.atStartOfDay(zone).toInstant();

        List<Budget> currentBudgets = budgetRepo.findByUserIdAndMonthStartOrderByCategoryAsc(userId, currentMonthStart);

        BigDecimal budgetLimit = currentBudgets.stream()
                .map(Budget::getLimitAmount)
                .map(BudgetService::nz)
                .reduce(ZERO, BigDecimal::add);

        if (budgetLimit.compareTo(ZERO) <= 0) {
            budgetLimit = null;
        }

        BigDecimal spendCurrentMonth = nz(txRepo.sumAmountForUserBetween(userId, currentMonthStartI, nextMonthStartI));

        BigDecimal budgetUsage = null;
        if (budgetLimit != null && budgetLimit.compareTo(ZERO) > 0) {
            budgetUsage = spendCurrentMonth.divide(budgetLimit, 6, RoundingMode.HALF_UP);
        } else {
            reasons.add("NO_CURRENT_MONTH_BUDGET");
        }

        Instant since30d = Instant.now().minus(Duration.ofDays(30));
        int maxOpenRisk = riskRepo.maxOpenRiskLevelLast30Days(userId, since30d);
        if (maxOpenRisk == 0) reasons.add("NO_OPEN_RISK_CASES_LAST_30D");

        LocalDate m1Start = currentMonthStart.minusMonths(1);
        LocalDate m2Start = currentMonthStart.minusMonths(2);
        LocalDate m3Start = currentMonthStart.minusMonths(3);

        Instant m1StartI = m1Start.atStartOfDay(zone).toInstant();
        Instant m2StartI = m2Start.atStartOfDay(zone).toInstant();
        Instant m3StartI = m3Start.atStartOfDay(zone).toInstant();

        long distinctMonths = txRepo.countDistinctYearMonthForUserBetween(userId, m3StartI, currentMonthStartI);

        String baseMethod;
        BigDecimal base;

        if (distinctMonths >= 3) {
            BigDecimal s3 = nz(txRepo.sumAmountForUserBetween(userId, m3StartI, m2StartI));
            BigDecimal s2 = nz(txRepo.sumAmountForUserBetween(userId, m2StartI, m1StartI));
            BigDecimal s1 = nz(txRepo.sumAmountForUserBetween(userId, m1StartI, currentMonthStartI));

            base = s1.add(s2).add(s3).divide(new BigDecimal("3"), 6, RoundingMode.HALF_UP);
            baseMethod = "3M_AVG";
            reasons.add("BASE_FROM_LAST_3_COMPLETE_MONTHS");
        } else {
            baseMethod = "BUDGET";
            if (budgetLimit != null) {
                base = budgetLimit;
                reasons.add("BASE_FROM_CURRENT_MONTH_BUDGET");
            } else {
                base = ZERO;
                reasons.add("NO_BASELINE_BUDGET_OR_HISTORY");
            }
        }

        BigDecimal usageComponent = ZERO;
        if (budgetUsage != null) {
            BigDecimal cappedUsage = budgetUsage.min(BD_1_5);
            usageComponent = cappedUsage.divide(BD_1_5, 6, RoundingMode.HALF_UP).multiply(BD_100);

            if (budgetUsage.compareTo(BD_1_5) > 0) {
                reasons.add("BUDGET_USAGE_CAPPED_AT_150_PERCENT");
            }
        }

        BigDecimal riskScore = BD_0_6.multiply(new BigDecimal(String.valueOf(maxOpenRisk)))
                .add(BD_0_4.multiply(usageComponent));

        riskScore = clamp(riskScore, ZERO, BD_100);

        BigDecimal factor = ONE.subtract(
                BD_0_8.multiply(riskScore.divide(BD_100, 10, RoundingMode.HALF_UP))
        );

        BigDecimal trustBudget = base.multiply(factor);

        BigDecimal maxTB = base.multiply(BD_2);
        trustBudget = clamp(trustBudget, BD_200, maxTB);

        if (budgetUsage != null && budgetUsage.compareTo(ONE) > 0) reasons.add("OVER_BUDGET_THIS_MONTH");
        if (riskScore.compareTo(new BigDecimal("60")) >= 0) reasons.add("HIGH_RISK_SCORE_REDUCES_TRUST");

        TrustBadgeDto.Response badge = badgeService.compute(userId);

        return new BudgetDto.TrustBudgetResponse(
                userId,
                true,
                baseMethod,
                money2(base),
                riskScore.setScale(2, RoundingMode.HALF_UP),
                budgetUsage == null ? null : budgetUsage.setScale(4, RoundingMode.HALF_UP),
                maxOpenRisk,
                money2(trustBudget),
                badge.level().name(),
                badge.reasons() == null ? List.of() : List.of(badge.reasons()),
                reasons
        );
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "GLOBAL";
        }
        return category.trim().toUpperCase();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }

    private static BigDecimal clamp(BigDecimal v, BigDecimal min, BigDecimal max) {
        if (v.compareTo(min) < 0) return min;
        if (v.compareTo(max) > 0) return max;
        return v;
    }

    private static BigDecimal money2(BigDecimal v) {
        return (v == null ? ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}