package tn.esprit.projet_pi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.dto.request.LoanRequestDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.LoanStatus;
import tn.esprit.projet_pi.exception.LoanAlreadyActiveException;
import tn.esprit.projet_pi.exception.LoanNotFoundException;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final AmortizationService amortizationService;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public LoanService(LoanRepository loanRepository,
            AmortizationService amortizationService,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.loanRepository = loanRepository;
        this.amortizationService = amortizationService;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    // ──────────────────────────────────────────
    // a) Créer un prêt depuis un DTO
    // ──────────────────────────────────────────
    public Loan createLoan(LoanRequestDTO dto, int riskScore, java.math.BigDecimal interestRate) {
        Loan loan = Loan.builder()
                .userId(dto.getUserId())
                .loanType(dto.getLoanType())
                .principalAmount(dto.getPrincipalAmount())
                .durationMonths(dto.getDurationMonths())
                .riskScore(riskScore)
                .interestRate(interestRate)
                .status(LoanStatus.PENDING)
                .build();

        return loanRepository.save(loan);
    }

    // ──────────────────────────────────────────
    // b) Approuver un prêt
    // ──────────────────────────────────────────
    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new LoanAlreadyActiveException("Loan is not in PENDING status");
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setStartDate(LocalDate.now());
        loan = loanRepository.save(loan);

        amortizationService.generateSchedule(loan);

        return loan;
    }

    // ──────────────────────────────────────────
    // c) Rejeter un prêt
    // ──────────────────────────────────────────
    public Loan rejectLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        loan.setStatus(LoanStatus.CLOSED);
        return loanRepository.save(loan);
    }

    // ──────────────────────────────────────────
    // d) Prêts d'un utilisateur
    // ──────────────────────────────────────────
    public List<Loan> getLoansByUser(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    // ──────────────────────────────────────────
    // e) Prêt par id
    // ──────────────────────────────────────────
    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    // ──────────────────────────────────────────
    // f) Échéancier d'un prêt
    // ──────────────────────────────────────────
    public List<RepaymentSchedule> getScheduleByLoan(Long loanId) {
        return repaymentScheduleRepository.findByLoanIdOrdered(loanId);
    }

    // ──────────────────────────────────────────
    // Méthodes CRUD héritées (compatibilité)
    // ──────────────────────────────────────────
    public Loan saveLoan(Loan loan) {
        return loanRepository.save(loan);
    }

    public Page<Loan> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    public Loan updateLoan(Long id, Loan loan) {
        Loan existing = getLoanById(id);
        existing.setPrincipalAmount(loan.getPrincipalAmount());
        existing.setInterestRate(loan.getInterestRate());
        existing.setDurationMonths(loan.getDurationMonths());
        existing.setMonthlyPayment(loan.getMonthlyPayment());
        existing.setStartDate(loan.getStartDate());
        existing.setStatus(loan.getStatus());
        return loanRepository.save(existing);
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
    }
}
