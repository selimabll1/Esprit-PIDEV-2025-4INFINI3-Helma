package tn.esprit.projet_pi.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueDetectionJob.class);

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public OverdueDetectionJob(RepaymentScheduleRepository repaymentScheduleRepository) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    /**
     * Tous les jours à 01h00 : passe en OVERDUE toutes les échéances
     * dont la date est dépassée et dont le statut est encore PENDING.
     */
    @Scheduled(cron = "0 0 1 * * *")
    void detectOverdue() {
        List<RepaymentSchedule> overdueSchedules = repaymentScheduleRepository.findOverdueSchedules(LocalDate.now());

        for (RepaymentSchedule schedule : overdueSchedules) {
            schedule.setStatus(PaymentStatus.OVERDUE);
            repaymentScheduleRepository.save(schedule);
        }

        log.info("Overdue detection : {} échéances mises à jour", overdueSchedules.size());
    }
}
