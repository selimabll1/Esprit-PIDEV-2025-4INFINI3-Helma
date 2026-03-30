package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid; // 🔥 IMPORTANT
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.request.LoanRequestDTO;
import tn.esprit.projet_pi.dto.response.AIDecisionResponseDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.service.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;
    private final RiskScoringService riskScoringService;
    private final InterestRateService interestRateService;
    private final AIDecisionService aiDecisionService;
    private final MLPredictionService mlPredictionService;
    private final MarkovService markovService;

    public LoanController(LoanService loanService,
                          RiskScoringService riskScoringService,
                          InterestRateService interestRateService,
                          AIDecisionService aiDecisionService,
                          MLPredictionService mlPredictionService,
                          MarkovService markovService) {
        this.loanService = loanService;
        this.riskScoringService = riskScoringService;
        this.interestRateService = interestRateService;
        this.aiDecisionService = aiDecisionService;
        this.mlPredictionService = mlPredictionService;
        this.markovService = markovService;
    }

    // 🔥 CREATE LOAN (VALIDATION ICI)
    @PostMapping
    public ResponseEntity<Loan> createLoan(@Valid @RequestBody LoanRequestDTO dto) {

        int riskScore = riskScoringService.calculateRiskScore(
                dto.getUserId(),
                dto.getPrincipalAmount(),
                dto.getDurationMonths(),
                dto.getLoanType()
        );

        BigDecimal interestRate = interestRateService.calculateRate(
                dto.getUserId(),
                dto.getPrincipalAmount(),
                dto.getLoanType()
        );

        Loan loan = loanService.createLoan(dto, riskScore, interestRate);
        return ResponseEntity.status(201).body(loan);
    }

    // GET /api/loans/{id}
    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    // GET /api/loans/user/{userId}
    @GetMapping("/user/{userId}")
    public List<Loan> getLoansByUser(@PathVariable Long userId) {
        return loanService.getLoansByUser(userId);
    }

    // GET /api/loans
    @GetMapping
    public ResponseEntity<Page<Loan>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    // PUT /api/loans/{id}/approve
    @PutMapping("/{id}/approve")
    public Loan approveLoan(@PathVariable Long id) {
        return loanService.approveLoan(id);
    }

    // PUT /api/loans/{id}/reject
    @PutMapping("/{id}/reject")
    public Loan rejectLoan(@PathVariable Long id) {
        return loanService.rejectLoan(id);
    }

    // GET /api/loans/{id}/schedule
    @GetMapping("/{id}/schedule")
    public List<RepaymentSchedule> getSchedule(@PathVariable Long id) {
        return loanService.getScheduleByLoan(id);
    }

    // GET /api/loans/{id}/summary
    @GetMapping("/{id}/summary")
    public Map<String, Object> getLoanSummary(@PathVariable Long id) {

        Loan loan = loanService.getLoanById(id);
        List<RepaymentSchedule> schedule = loanService.getScheduleByLoan(id);

        BigDecimal totalPaid = schedule.stream()
                .filter(s -> s.getStatus() == PaymentStatus.PAID)
                .map(RepaymentSchedule::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = schedule.stream()
                .filter(s -> s.getStatus() != PaymentStatus.PAID)
                .map(RepaymentSchedule::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RepaymentSchedule nextPayment = schedule.stream()
                .filter(s -> s.getStatus() == PaymentStatus.PENDING
                        || s.getStatus() == PaymentStatus.OVERDUE)
                .findFirst()
                .orElse(null);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("loan", loan);
        summary.put("totalPaid", totalPaid);
        summary.put("totalRemaining", totalRemaining);
        summary.put("nextPayment", nextPayment);

        return summary;
    }

    // AI Decision
    @PostMapping("/{id}/ai-decision")
    public ResponseEntity<AIDecisionResponseDTO> getAIDecision(@PathVariable Long id) {
        return ResponseEntity.ok(aiDecisionService.analyzeCredit(id));
    }

    // ML Prediction
    @PostMapping("/{id}/ml-predict")
    public ResponseEntity<Map<String, Object>> getMLPrediction(@PathVariable Long id) {
        return ResponseEntity.ok(mlPredictionService.predictLoan(id));
    }

    // Markov Prediction
    @PostMapping("/{id}/markov-predict")
    public ResponseEntity<Map<String, Object>> getMarkovPrediction(@PathVariable Long id) {
        return ResponseEntity.ok(markovService.predictMarkov(id));
    }
}