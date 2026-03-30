package tn.esprit.projet_pi.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.enums.LoanStatus;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.util.List;

@Component
public class DefaultDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(DefaultDetectionJob.class);

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public DefaultDetectionJob(LoanRepository loanRepository,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    /**
     * Tous les jours à 01h30 : passe en DEFAULTED tout prêt ACTIVE
     * qui cumule 3 échéances OVERDUE ou plus.
     */
    @Scheduled(cron = "0 30 1 * * *")
    void detectDefaults() {
        List<Loan> activeLoans = loanRepository.findAllActiveLoans();
        int defaultedCount = 0;

        for (Loan loan : activeLoans) {
            int overdueCount = repaymentScheduleRepository
                    .countOverdueByLoanId(loan.getId());

            if (overdueCount >= 3) {
                loan.setStatus(LoanStatus.DEFAULTED);
                loanRepository.save(loan);
                defaultedCount++;
            }
        }

        log.info("Default detection : {} loans passés en DEFAULTED", defaultedCount);
    }
}
