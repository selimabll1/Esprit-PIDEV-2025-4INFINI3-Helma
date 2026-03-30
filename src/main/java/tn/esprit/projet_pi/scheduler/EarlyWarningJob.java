package tn.esprit.projet_pi.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.util.List;

@Component
public class EarlyWarningJob {

    private static final Logger log = LoggerFactory.getLogger(EarlyWarningJob.class);

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public EarlyWarningJob(LoanRepository loanRepository,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    /**
     * Tous les jours à 02h00 : émet un warning pour tout prêt à risk élevé
     * (riskScore > 60) ayant au moins 1 échéance OVERDUE.
     */
    @Scheduled(cron = "0 0 2 * * *")
    void detectEarlyWarnings() {
        List<Loan> highRiskLoans = loanRepository.findHighRiskActiveLoans(60);

        for (Loan loan : highRiskLoans) {
            int overdueCount = repaymentScheduleRepository
                    .countOverdueByLoanId(loan.getId());

            if (overdueCount >= 1) {
                log.warn("EARLY WARNING - Loan ID {} - User {} - OverdueCount {} - RiskScore {}",
                        loan.getId(),
                        loan.getUserId(),
                        overdueCount,
                        loan.getRiskScore());
            }
        }
    }
}
