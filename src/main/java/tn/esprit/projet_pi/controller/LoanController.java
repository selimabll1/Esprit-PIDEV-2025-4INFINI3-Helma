package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.dto.request.LoanRequestDTO;
import tn.esprit.projet_pi.dto.response.AIDecisionResponseDTO;
import tn.esprit.projet_pi.dto.response.FinancialHealthDTO;
import tn.esprit.projet_pi.dto.response.MultiAgentDecisionDTO;
import tn.esprit.projet_pi.entity.EarlyWarning;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.security.LoanAccessService;
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
    private final MultiAgentRiskService multiAgentRiskService;
    private final FinancialHealthService financialHealthService;
    private final PdfContractService pdfContractService;
    private final EarlyWarningService earlyWarningService;
    private final LoanAccessService loanAccessService;

    public LoanController(LoanService loanService,
            RiskScoringService riskScoringService,
            InterestRateService interestRateService,
            AIDecisionService aiDecisionService,
            MLPredictionService mlPredictionService,
            MarkovService markovService,
            MultiAgentRiskService multiAgentRiskService,
            FinancialHealthService financialHealthService,
            PdfContractService pdfContractService,
            EarlyWarningService earlyWarningService,
            LoanAccessService loanAccessService) {
        this.loanService = loanService;
        this.riskScoringService = riskScoringService;
        this.interestRateService = interestRateService;
        this.aiDecisionService = aiDecisionService;
        this.mlPredictionService = mlPredictionService;
        this.markovService = markovService;
        this.multiAgentRiskService = multiAgentRiskService;
        this.financialHealthService = financialHealthService;
        this.pdfContractService = pdfContractService;
        this.earlyWarningService = earlyWarningService;
        this.loanAccessService = loanAccessService;
    }

    @PostMapping
    public ResponseEntity<Loan> createLoan(@Valid @RequestBody LoanRequestDTO dto) {
        loanAccessService.assertCanCreateForUser(dto.getUserId());
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

    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        loanAccessService.assertCanAccessLoan(loan);
        return loan;
    }

    @GetMapping("/user/{userId}")
    public List<Loan> getLoansByUser(@PathVariable Long userId) {
        loanAccessService.assertCanAccessUser(userId);
        return loanService.getLoansByUser(userId);
    }

    @GetMapping
    public ResponseEntity<Page<Loan>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    @PutMapping("/{id}/approve")
    public Loan approveLoan(@PathVariable Long id) {
        return loanService.approveLoan(id);
    }

    @PutMapping("/{id}/reject")
    public Loan rejectLoan(@PathVariable Long id) {
        return loanService.rejectLoan(id);
    }

    @GetMapping("/{id}/schedule")
    public List<RepaymentSchedule> getSchedule(@PathVariable Long id) {
        loanAccessService.assertCanAccessLoan(loanService.getLoanById(id));
        return loanService.getScheduleByLoan(id);
    }

    @GetMapping("/{id}/summary")
    public Map<String, Object> getLoanSummary(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        loanAccessService.assertCanAccessLoan(loan);
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
                .filter(s -> s.getStatus() == PaymentStatus.PENDING || s.getStatus() == PaymentStatus.OVERDUE)
                .findFirst()
                .orElse(null);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("loan", loan);
        summary.put("totalPaid", totalPaid);
        summary.put("totalRemaining", totalRemaining);
        summary.put("nextPayment", nextPayment);
        return summary;
    }

    @PostMapping("/{id}/ai-decision")
    public ResponseEntity<AIDecisionResponseDTO> getAIDecision(@PathVariable Long id) {
        return ResponseEntity.ok(aiDecisionService.analyzeCredit(id));
    }

    @PostMapping("/{loanId}/multi-agent-decision")
    public ResponseEntity<MultiAgentDecisionDTO> getMultiAgentDecision(@PathVariable Long loanId) {
        try {
            return ResponseEntity.ok(multiAgentRiskService.decide(loanId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MultiAgentDecisionDTO.builder()
                    .loanId(loanId)
                    .finalDecision("REJECT")
                    .explanation("Erreur decision multi-agent : " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/users/{userId}/financial-health")
    public ResponseEntity<FinancialHealthDTO> getFinancialHealthCompat(@PathVariable Long userId) {
        loanAccessService.assertCanAccessUser(userId);
        return ResponseEntity.ok(financialHealthService.calculateScore(userId));
    }

    @GetMapping("/{id}/generate-contract")
    public ResponseEntity<byte[]> generateContract(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        loanAccessService.assertCanAccessLoan(loan);
        byte[] pdf = pdfContractService.generateContract(loan);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contrat_" + id + ".pdf")
                .body(pdf);
    }

    @GetMapping("/early-warnings")
    public ResponseEntity<List<EarlyWarning>> getEarlyWarnings() {
        return ResponseEntity.ok(earlyWarningService.getRiskyWarnings());
    }

    @PostMapping("/{id}/ml-predict")
    public ResponseEntity<Map<String, Object>> getMLPrediction(@PathVariable Long id) {
        return ResponseEntity.ok(mlPredictionService.predictLoan(id));
    }

    @PostMapping("/{id}/markov-predict")
    public ResponseEntity<Map<String, Object>> getMarkovPrediction(@PathVariable Long id) {
        return ResponseEntity.ok(markovService.predictMarkov(id));
    }
}
