package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.IncomeStatementDto;
import com.esprit.helma_backend.entities.CashFlowSummary;
import com.esprit.helma_backend.entities.IncomeStatement;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.CashFlowSummaryRepository;
import com.esprit.helma_backend.repositories.IncomeStatementRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class IncomeStatementService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeStatementRepository incomeStatementRepo;
    private final CashFlowSummaryRepository cashFlowSummaryRepo;
    private final UserRepository userRepo;

    public IncomeStatementService(IncomeStatementRepository incomeStatementRepo,
                                  CashFlowSummaryRepository cashFlowSummaryRepo,
                                  UserRepository userRepo) {
        this.incomeStatementRepo = incomeStatementRepo;
        this.cashFlowSummaryRepo = cashFlowSummaryRepo;
        this.userRepo = userRepo;
    }

    public IncomeStatementDto.Response recalculateForUser(Long userId, LocalDate month) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CashFlowSummary cashFlow = cashFlowSummaryRepo.findByUserIdAndMonthStart(userId, month)
                .orElse(null);

        if (cashFlow == null) {
            return null;
        }

        BigDecimal totalRevenue = nz(cashFlow.getTotalIncome()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalExpenses = nz(cashFlow.getTotalExpense()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netResult = totalRevenue.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);

        BigDecimal marginRate = null;
        if (totalRevenue.compareTo(ZERO) > 0) {
            marginRate = netResult
                    .divide(totalRevenue, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        IncomeStatement statement = incomeStatementRepo.findByUserIdAndMonthStart(userId, month)
                .orElseGet(IncomeStatement::new);

        statement.setUser(user);
        statement.setMonthStart(month);
        statement.setTotalRevenue(totalRevenue);
        statement.setTotalExpenses(totalExpenses);
        statement.setNetResult(netResult);
        statement.setMarginRate(marginRate);

        IncomeStatement saved = incomeStatementRepo.save(statement);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncomeStatementDto.Response> getHistory(Long userId) {
        return incomeStatementRepo.findByUserIdOrderByMonthStartAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncomeStatementDto.Response getCurrent(Long userId) {
        return incomeStatementRepo.findTopByUserIdOrderByMonthStartDesc(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("No income statement found for user " + userId));
    }

    private IncomeStatementDto.Response toResponse(IncomeStatement s) {
        return new IncomeStatementDto.Response(
                s.getId(),
                s.getUser().getId(),
                s.getMonthStart(),
                s.getTotalRevenue(),
                s.getTotalExpenses(),
                s.getNetResult(),
                s.getMarginRate(),
                s.getUpdatedAt()
        );
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}