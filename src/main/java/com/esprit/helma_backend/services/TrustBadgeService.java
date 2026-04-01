package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.TrustBadgeDto;
import com.esprit.helma_backend.entities.BadgeLevel;
import com.esprit.helma_backend.entities.TrustBadge;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.CashFlowSummaryRepository;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.SavingsGoalRepository;
import com.esprit.helma_backend.repositories.TrustBadgeRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TrustBadgeService {

    private static final BigDecimal CREDIT_UNVERIFIED = new BigDecimal("0.00");
    private static final BigDecimal CREDIT_BUILDING   = new BigDecimal("500.00");
    private static final BigDecimal CREDIT_TRUSTED    = new BigDecimal("2000.00");
    private static final BigDecimal CREDIT_ELITE      = new BigDecimal("5000.00");

    private final TrustBadgeRepository trustBadgeRepository;
    private final UserRepository userRepository;
    private final CashFlowSummaryRepository cashFlowSummaryRepository;
    private final RiskCaseRepository riskCaseRepository;
    private final BurnRateService burnRateService;
    private final SavingsGoalRepository savingsGoalRepo;
    private final AuditLogService auditLogService;

    public TrustBadgeService(TrustBadgeRepository trustBadgeRepository,
                             UserRepository userRepository,
                             CashFlowSummaryRepository cashFlowSummaryRepository,
                             RiskCaseRepository riskCaseRepository,
                             BurnRateService burnRateService,
                             SavingsGoalRepository savingsGoalRepo,
                             AuditLogService auditLogService) {
        this.trustBadgeRepository = trustBadgeRepository;
        this.userRepository = userRepository;
        this.cashFlowSummaryRepository = cashFlowSummaryRepository;
        this.riskCaseRepository = riskCaseRepository;
        this.burnRateService = burnRateService;
        this.savingsGoalRepo = savingsGoalRepo;
        this.auditLogService = auditLogService;
    }

    public TrustBadgeDto.Response compute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        long monthsOfHistory = cashFlowSummaryRepository.countMonthsOfHistoryByUserId(userId);
        long openRiskCases = riskCaseRepository.countOpenByUserId(userId);
        int maxOpenRiskLast30Days = riskCaseRepository.maxOpenRiskLevelLast30Days(
                userId,
                Instant.now().minus(30, ChronoUnit.DAYS)
        );
        BigDecimal runwayMonths = burnRateService.compute(userId).runwayMonths();
        int savingsGoalsCompleted = savingsGoalRepo.countByUserIdAndCompletedTrue(userId);

        List<String> reasons = new ArrayList<>();
        reasons.add("MONTHS_HISTORY_" + monthsOfHistory);
        reasons.add("OPEN_RISK_CASES_" + openRiskCases);
        reasons.add("MAX_OPEN_RISK_30D_" + maxOpenRiskLast30Days);
        reasons.add("RUNWAY_" + (runwayMonths != null ? runwayMonths.toPlainString() : "NULL"));
        reasons.add("SAVINGS_GOALS_COMPLETED_" + savingsGoalsCompleted);

        BadgeLevel level = resolveLevel(
                monthsOfHistory,
                openRiskCases,
                maxOpenRiskLast30Days,
                runwayMonths,
                savingsGoalsCompleted,
                reasons
        );

        BigDecimal creditCapacity = resolveCreditCapacity(level);
        Instant now = Instant.now();

        TrustBadge badge = trustBadgeRepository.findByUserId(userId)
                .orElseGet(TrustBadge::new);

        badge.setUser(user);
        badge.setLevel(level);
        badge.setCreditCapacity(creditCapacity);
        badge.setComputedAt(now);
        badge.setReasons(String.join(",", reasons));

        TrustBadge saved = trustBadgeRepository.save(badge);

        auditLogService.log(
                userId,
                "BADGE_COMPUTED",
                "TrustBadge",
                saved.getId(),
                "level=" + saved.getLevel()
                        + ",creditCapacity=" + saved.getCreditCapacity()
        );

        return new TrustBadgeDto.Response(
                saved.getUser().getId(),
                saved.getLevel(),
                saved.getCreditCapacity(),
                saved.getReasons(),
                saved.getComputedAt()
        );
    }

    @Transactional(readOnly = true)
    public TrustBadgeDto.Response getByUserId(Long userId) {
        TrustBadge badge = trustBadgeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("TrustBadge not found for user: " + userId));

        return new TrustBadgeDto.Response(
                badge.getUser().getId(),
                badge.getLevel(),
                badge.getCreditCapacity(),
                badge.getReasons(),
                badge.getComputedAt()
        );
    }

    private BadgeLevel resolveLevel(long monthsOfHistory,
                                    long openRiskCases,
                                    int maxOpenRiskLast30Days,
                                    BigDecimal runwayMonths,
                                    int savingsGoalsCompleted,
                                    List<String> reasons) {

        if (monthsOfHistory < 1) {
            reasons.add("LEVEL_UNVERIFIED");
            return BadgeLevel.UNVERIFIED;
        }

        if (monthsOfHistory >= 6
                && openRiskCases == 0
                && runwayMonths != null
                && runwayMonths.compareTo(new BigDecimal("6")) >= 0
                && savingsGoalsCompleted >= 1) {
            reasons.add("LEVEL_ELITE");
            return BadgeLevel.ELITE;
        }

        if (monthsOfHistory >= 3
                && runwayMonths != null
                && runwayMonths.compareTo(new BigDecimal("3")) >= 0
                && openRiskCases <= 1
                && maxOpenRiskLast30Days < 50) {
            reasons.add("LEVEL_TRUSTED");
            return BadgeLevel.TRUSTED;
        }

        if (monthsOfHistory >= 1
                && runwayMonths != null
                && runwayMonths.compareTo(new BigDecimal("1")) >= 0) {
            reasons.add("LEVEL_BUILDING");
            return BadgeLevel.BUILDING;
        }

        reasons.add("LEVEL_BUILDING");
        return BadgeLevel.BUILDING;
    }

    private BigDecimal resolveCreditCapacity(BadgeLevel level) {
        return switch (level) {
            case UNVERIFIED -> CREDIT_UNVERIFIED;
            case BUILDING -> CREDIT_BUILDING;
            case TRUSTED -> CREDIT_TRUSTED;
            case ELITE -> CREDIT_ELITE;
        };
    }
}