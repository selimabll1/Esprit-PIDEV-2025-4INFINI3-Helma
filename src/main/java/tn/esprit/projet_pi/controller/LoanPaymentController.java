package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid; // 🔥 IMPORTANT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.request.LoanPaymentDTO;
import tn.esprit.projet_pi.entity.LoanPayment;
import tn.esprit.projet_pi.repository.LoanPaymentRepository;
import tn.esprit.projet_pi.service.RepaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/loans/payments")
@CrossOrigin(origins = "*")
public class LoanPaymentController {

    private final RepaymentService repaymentService;
    private final LoanPaymentRepository loanPaymentRepository;

    public LoanPaymentController(RepaymentService repaymentService,
                                 LoanPaymentRepository loanPaymentRepository) {
        this.repaymentService = repaymentService;
        this.loanPaymentRepository = loanPaymentRepository;
    }

    // 🔥 PAY INSTALLMENT (VALIDATION ICI)
    @PostMapping
    public ResponseEntity<LoanPayment> payInstallment(@Valid @RequestBody LoanPaymentDTO dto) {
        LoanPayment payment = repaymentService.payInstallment(dto);
        return ResponseEntity.status(201).body(payment);
    }

    // GET /api/loans/payments/{loanId}
    @GetMapping("/{loanId}")
    public ResponseEntity<List<LoanPayment>> getPaymentsByLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanPaymentRepository.findByLoanId(loanId));
    }
}