package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.dto.response.MultiAgentDecisionDTO;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.repository.LoanRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class MultiAgentRiskService {

    private final GroqService groqService;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public MultiAgentRiskService(GroqService groqService,
            LoanRepository loanRepository,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.groqService = groqService;
        this.loanRepository = loanRepository;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    public MultiAgentDecisionDTO decide(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Pret introuvable: " + loanId));
        List<RepaymentSchedule> schedule = repaymentScheduleRepository.findByLoanIdOrdered(loanId);
        long overdue = schedule.stream().filter(s -> "OVERDUE".equals(String.valueOf(s.getStatus()))).count();
        long paid = schedule.stream().filter(s -> "PAID".equals(String.valueOf(s.getStatus()))).count();
        long loansLast30Days = loanRepository.countLoansByUserSince(loan.getUserId(), LocalDate.now().minusDays(30));

        String credit = groqService.ask("""
                Tu es CreditScoringAgent pour HELMA. Analyse en francais le score risque et l'historique paiements.
                Retourne un court JSON texte avec score_ajuste (0-100), flag_critique (true/false) et justification.
                Donnees: riskScore=%s, echeances payees=%s, echeances en retard=%s, montant=%s TND, duree=%s mois.
                """.formatted(loan.getRiskScore(), paid, overdue, loan.getPrincipalAmount(), loan.getDurationMonths()));

        String fraud = groqService.ask("""
                Tu es FraudDetectionAgent pour HELMA. Analyse les demandes recentes et montants inhabituels.
                Retourne fraudRisk LOW/MEDIUM/HIGH avec justification en francais.
                Donnees: prets soumis 30 derniers jours=%s, montant=%s TND, type=%s, userId=%s.
                """.formatted(loansLast30Days, loan.getPrincipalAmount(), loan.getLoanType(), loan.getUserId()));

        String market = groqService.ask("""
                Tu es MarketContextAgent. Donne le contexte economique actuel de la microfinance en Tunisie
                en mentionnant taux BNA, inflation et impact sur le risque credit. Reponse concise en francais.
                """);

        String finalDecision = groqService.ask("""
                Tu es FinalDecisionAgent HELMA. Sur la base des trois analyses ci-dessous, retourne une decision finale
                APPROVE ou REJECT, avec une explication complete en francais.
                CreditScoringAgent: %s
                FraudDetectionAgent: %s
                MarketContextAgent: %s
                """.formatted(credit, fraud, market));

        String normalizedDecision = finalDecision.toUpperCase().contains("REJECT") ? "REJECT" : "APPROVE";
        return MultiAgentDecisionDTO.builder()
                .loanId(loanId)
                .originalRiskScore(loan.getRiskScore())
                .creditScoringAnalysis(credit)
                .adjustedScore(extractFirstInt(credit, loan.getRiskScore()))
                .criticalFlag(credit.toLowerCase().contains("true") || credit.toLowerCase().contains("critique"))
                .fraudDetectionAnalysis(fraud)
                .fraudRisk(extractFraudRisk(fraud))
                .marketContextAnalysis(market)
                .marketContext(market)
                .finalDecisionAnalysis(finalDecision)
                .finalDecision(normalizedDecision)
                .explanation(finalDecision)
                .build();
    }

    private Integer extractFirstInt(String text, Integer fallback) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(text);
            return matcher.find() ? Integer.parseInt(matcher.group()) : fallback;
        } catch (Exception e) {
            log.warn("Impossible d'extraire score ajuste: {}", e.getMessage());
            return fallback;
        }
    }

    private String extractFraudRisk(String text) {
        String upper = text.toUpperCase();
        if (upper.contains("HIGH")) return "HIGH";
        if (upper.contains("MEDIUM")) return "MEDIUM";
        return "LOW";
    }
}
