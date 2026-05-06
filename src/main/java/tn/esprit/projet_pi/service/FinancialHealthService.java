package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.dto.response.FinancialHealthDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.LoanStatus;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class FinancialHealthService {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public FinancialHealthService(LoanRepository loanRepository,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    public FinancialHealthDTO calculateScore(Long userId) {
        try {
            List<Loan> loans = loanRepository.findByUserId(userId);
            int repaymentHistory = calculateRepaymentHistory(loans);
            int regularity = calculateRegularity(loans);
            int debtRatio = calculateDebtRatio(loans);
            int seniority = calculateSeniority(loans);
            int loanType = calculateLoanType(loans);

            int score = (int) Math.round(1000 * (
                    repaymentHistory / 300.0 * 0.30
                            + regularity / 250.0 * 0.25
                            + debtRatio / 200.0 * 0.20
                            + seniority / 150.0 * 0.15
                            + loanType / 100.0 * 0.10)
            );
            score = Math.max(0, Math.min(1000, score));
            String level = level(score);

            return FinancialHealthDTO.builder()
                    .userId(userId)
                    .helmaScore(score)
                    .level(level)
                    .maxLoanAmount(BigDecimal.valueOf(score).multiply(BigDecimal.valueOf(25)).setScale(2, RoundingMode.HALF_UP))
                    .preferentialRate(score >= 800)
                    .details(FinancialHealthDTO.Details.builder()
                            .repaymentHistory(repaymentHistory)
                            .regularity(regularity)
                            .debtRatio(debtRatio)
                            .seniority(seniority)
                            .loanType(loanType)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Erreur calcul Financial Health pour user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de calculer le score de sante financiere: " + e.getMessage());
        }
    }

    private int calculateRepaymentHistory(List<Loan> loans) {
        if (loans.isEmpty()) return 180;
        long defaulted = loans.stream().filter(l -> l.getStatus() == LoanStatus.DEFAULTED).count();
        long closed = loans.stream().filter(l -> l.getStatus() == LoanStatus.CLOSED).count();
        int score = 220 + (int) closed * 20 - (int) defaulted * 90;
        return clamp(score, 0, 300);
    }

    private int calculateRegularity(List<Loan> loans) {
        long paid = 0;
        long late = 0;
        for (Loan loan : loans) {
            List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrdered(loan.getId());
            paid += schedules.stream().filter(s -> s.getStatus() == PaymentStatus.PAID).count();
            late += schedules.stream().filter(s -> s.getStatus() == PaymentStatus.OVERDUE).count();
        }
        if (paid + late == 0) return 160;
        return clamp((int) Math.round(250.0 * paid / (paid + late)), 0, 250);
    }

    private int calculateDebtRatio(List<Loan> loans) {
        BigDecimal activeDebt = loans.stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int avgDuration = loans.stream()
                .filter(l -> l.getDurationMonths() != null)
                .mapToInt(Loan::getDurationMonths)
                .findFirst()
                .orElse(12);
        int penalty = activeDebt.divide(BigDecimal.valueOf(Math.max(avgDuration, 1)), 0, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(50), 0, RoundingMode.HALF_UP)
                .intValue();
        return clamp(200 - penalty, 0, 200);
    }

    private int calculateSeniority(List<Loan> loans) {
        LocalDate first = loans.stream()
                .map(Loan::getStartDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        long months = ChronoUnit.MONTHS.between(first, LocalDate.now());
        return clamp((int) months * 12, 0, 150);
    }

    private int calculateLoanType(List<Loan> loans) {
        long businessOrStudent = loans.stream()
                .filter(l -> l.getLoanType() != null && !"PERSONAL".equals(l.getLoanType().name()))
                .count();
        return clamp(70 + (int) businessOrStudent * 10, 0, 100);
    }

    private String level(int score) {
        if (score >= 800) return "EXCELLENT";
        if (score >= 600) return "GOOD";
        if (score >= 400) return "FAIR";
        return "POOR";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
