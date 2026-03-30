package tn.esprit.projet_pi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.response.LoanStatisticsDTO;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/loans/statistics")
@CrossOrigin(origins = "*")
public class LoanStatisticsController {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    public LoanStatisticsController(LoanRepository loanRepository,
                                    RepaymentScheduleRepository scheduleRepository) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
    }

    // GET /api/loans/statistics
    @GetMapping
    public ResponseEntity<LoanStatisticsDTO> getStatistics() {

        long totalLoans = loanRepository.count();
        long activeLoans = loanRepository.findAllActiveLoans().size();
        long defaultedLoans = loanRepository.findAllDefaultedLoans().size();

        // 🔥 Calcul PAR
        List<RepaymentSchedule> schedules = scheduleRepository.findAll();

        long overdue30 = schedules.stream()
                .filter(s -> s.getStatus() == PaymentStatus.OVERDUE
                        && s.getDueDate().isBefore(LocalDate.now().minusDays(30)))
                .count();

        long overdue60 = schedules.stream()
                .filter(s -> s.getStatus() == PaymentStatus.OVERDUE
                        && s.getDueDate().isBefore(LocalDate.now().minusDays(60)))
                .count();

        long overdue90 = schedules.stream()
                .filter(s -> s.getStatus() == PaymentStatus.OVERDUE
                        && s.getDueDate().isBefore(LocalDate.now().minusDays(90)))
                .count();

        double par30 = totalLoans == 0 ? 0 : (double) overdue30 / totalLoans;
        double par60 = totalLoans == 0 ? 0 : (double) overdue60 / totalLoans;
        double par90 = totalLoans == 0 ? 0 : (double) overdue90 / totalLoans;

        LoanStatisticsDTO stats = new LoanStatisticsDTO(
                totalLoans,
                activeLoans,
                defaultedLoans,
                par30,
                par60,
                par90
        );

        return ResponseEntity.ok(stats);
    }
}