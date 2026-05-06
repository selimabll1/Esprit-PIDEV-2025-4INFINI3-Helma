package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid; // 🔥 IMPORTANT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.request.LoanPaymentDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.LoanPayment;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.LoanPaymentRepository;
import tn.esprit.projet_pi.security.LoanAccessService;
import tn.esprit.projet_pi.service.RepaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/loans/payments")
@CrossOrigin(origins = "*")
public class LoanPaymentController {

    private final RepaymentService repaymentService;
    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanAccessService loanAccessService;

    public LoanPaymentController(RepaymentService repaymentService,
                                 LoanPaymentRepository loanPaymentRepository,
                                 LoanRepository loanRepository,
                                 LoanAccessService loanAccessService) {
        this.repaymentService = repaymentService;
        this.loanPaymentRepository = loanPaymentRepository;
        this.loanRepository = loanRepository;
        this.loanAccessService = loanAccessService;
    }

    // 🔥 PAY INSTALLMENT (VALIDATION ICI)
    @PostMapping
    public ResponseEntity<LoanPayment> payInstallment(@Valid @RequestBody LoanPaymentDTO dto) {
        Loan loan = loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loanAccessService.assertCanAccessLoan(loan);
        LoanPayment payment = repaymentService.payInstallment(dto);
        return ResponseEntity.status(201).body(payment);
    }

    // GET /api/loans/payments/{loanId}
    @GetMapping("/{loanId}")
    public ResponseEntity<List<LoanPayment>> getPaymentsByLoan(@PathVariable Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loanAccessService.assertCanAccessLoan(loan);
        return ResponseEntity.ok(loanPaymentRepository.findByLoanId(loanId));
    }
}
