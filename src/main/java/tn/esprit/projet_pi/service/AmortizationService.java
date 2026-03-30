package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmortizationService {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public AmortizationService(RepaymentScheduleRepository repaymentScheduleRepository) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    /**
     * Génère et sauvegarde l'échéancier d'amortissement d'un prêt.
     * Formule PMT : M = P * [r(1+r)^n] / [(1+r)^n - 1]
     * Si r == 0 : M = P / n
     */
    public List<RepaymentSchedule> generateSchedule(Loan loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        int n = loan.getDurationMonths();
        // interestRate est un taux annuel en % (ex: 5.0 => 5 %)
        BigDecimal annualRate = loan.getInterestRate();

        BigDecimal monthlyPayment;

        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            // Taux zéro : mensualité = principal / durée
            monthlyPayment = principal.divide(BigDecimal.valueOf(n), 3, RoundingMode.HALF_UP);
        } else {
            // r = taux mensuel
            BigDecimal r = annualRate
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            // (1+r)^n
            BigDecimal onePlusR = BigDecimal.ONE.add(r);
            BigDecimal pow = onePlusR.pow(n, new MathContext(15, RoundingMode.HALF_UP));

            // P * r * (1+r)^n / ((1+r)^n - 1)
            BigDecimal numerator = principal.multiply(r).multiply(pow);
            BigDecimal denominator = pow.subtract(BigDecimal.ONE);

            monthlyPayment = numerator.divide(denominator, 3, RoundingMode.HALF_UP);
        }

        List<RepaymentSchedule> schedules = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loanId(loan.getId())
                    .installmentNumber(i + 1)
                    .dueDate(loan.getStartDate().plusMonths(i + 1))
                    .expectedAmount(monthlyPayment)
                    .paidAmount(BigDecimal.ZERO)
                    .status(PaymentStatus.PENDING)
                    .build();

            schedules.add(repaymentScheduleRepository.save(schedule));
        }

        return schedules;
    }
}
