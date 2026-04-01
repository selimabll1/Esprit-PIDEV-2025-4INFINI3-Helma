package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.CashFlowDto;
import com.esprit.helma_backend.entities.CashFlowSummary;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.CashFlowSummaryRepository;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CashFlowService {

    private final CashFlowSummaryRepository cashFlowRepo;
    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final IncomeStatementService incomeStatementService;

    public CashFlowService(CashFlowSummaryRepository cashFlowRepo,
                           TransactionRepository txRepo,
                           UserRepository userRepo,
                           IncomeStatementService incomeStatementService) {
        this.cashFlowRepo = cashFlowRepo;
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.incomeStatementService = incomeStatementService;
    }

    public CashFlowDto.Response recalculateForUser(Long userId, LocalDate monthStart) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate normalizedMonthStart = monthStart.withDayOfMonth(1);

        ZoneId zone = ZoneId.systemDefault();
        LocalDate nextMonth = normalizedMonthStart.plusMonths(1);

        Instant from = normalizedMonthStart.atStartOfDay(zone).toInstant();
        Instant to = nextMonth.atStartOfDay(zone).toInstant();

        BigDecimal totalIncome = nz(txRepo.sumIncomeForUserBetween(userId, from, to));
        BigDecimal totalExpense = nz(txRepo.sumExpenseForUserBetween(userId, from, to));
        BigDecimal netFlow = totalIncome.subtract(totalExpense);

        Optional<CashFlowSummary> prevOpt =
                cashFlowRepo.findTopByUserIdAndMonthStartLessThanOrderByMonthStartDesc(userId, normalizedMonthStart);
        BigDecimal prevBalance = prevOpt.map(CashFlowSummary::getCumulativeBalance).orElse(BigDecimal.ZERO);

        if (totalIncome.compareTo(BigDecimal.ZERO) == 0 && totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            cashFlowRepo.findByUserIdAndMonthStart(userId, normalizedMonthStart)
                    .ifPresent(cashFlowRepo::delete);

            incomeStatementService.recalculateForUser(userId, normalizedMonthStart);

            return new CashFlowDto.Response(
                    null,
                    userId,
                    normalizedMonthStart,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    prevBalance,
                    Instant.now()
            );
        }

        BigDecimal cumulativeBalance = prevBalance.add(netFlow);

        CashFlowSummary summary = cashFlowRepo
                .findByUserIdAndMonthStart(userId, normalizedMonthStart)
                .orElse(CashFlowSummary.builder()
                        .user(user)
                        .monthStart(normalizedMonthStart)
                        .build());

        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetFlow(netFlow);
        summary.setCumulativeBalance(cumulativeBalance);

        incomeStatementService.recalculateForUser(userId, normalizedMonthStart);

        return toResponse(cashFlowRepo.save(summary));
    }

    public void recalculateRangeForUser(Long userId, LocalDate fromMonthStart, LocalDate toMonthStart) {
        if (fromMonthStart == null || toMonthStart == null) {
            return;
        }

        LocalDate start = fromMonthStart.withDayOfMonth(1);
        LocalDate end = toMonthStart.withDayOfMonth(1);

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            recalculateForUser(userId, cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    @Transactional(readOnly = true)
    public CashFlowDto.Response getCurrentMonth(Long userId) {
        LocalDate monthStart = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1);

        return cashFlowRepo.findByUserIdAndMonthStart(userId, monthStart)
                .map(this::toResponse)
                .orElseGet(() -> {
                    ZoneId zone = ZoneId.systemDefault();
                    LocalDate nextMonth = monthStart.plusMonths(1);

                    Instant from = monthStart.atStartOfDay(zone).toInstant();
                    Instant to = nextMonth.atStartOfDay(zone).toInstant();

                    BigDecimal totalIncome = nz(txRepo.sumIncomeForUserBetween(userId, from, to));
                    BigDecimal totalExpense = nz(txRepo.sumExpenseForUserBetween(userId, from, to));
                    BigDecimal netFlow = totalIncome.subtract(totalExpense);

                    BigDecimal prevBalance = cashFlowRepo
                            .findTopByUserIdAndMonthStartLessThanOrderByMonthStartDesc(userId, monthStart)
                            .map(CashFlowSummary::getCumulativeBalance)
                            .orElse(BigDecimal.ZERO);

                    return new CashFlowDto.Response(
                            null,
                            userId,
                            monthStart,
                            totalIncome,
                            totalExpense,
                            netFlow,
                            prevBalance.add(netFlow),
                            Instant.now()
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<CashFlowDto.Response> getHistory(Long userId) {
        return cashFlowRepo.findByUserIdOrderByMonthStartAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CashFlowDto.Response toResponse(CashFlowSummary c) {
        return new CashFlowDto.Response(
                c.getId(),
                c.getUser().getId(),
                c.getMonthStart(),
                c.getTotalIncome(),
                c.getTotalExpense(),
                c.getNetFlow(),
                c.getCumulativeBalance(),
                c.getUpdatedAt()
        );
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}