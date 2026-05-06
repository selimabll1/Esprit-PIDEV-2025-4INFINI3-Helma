package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.projet_pi.entity.EarlyWarning;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.enums.EarlyWarningStatus;
import tn.esprit.projet_pi.repository.EarlyWarningRepository;
import tn.esprit.projet_pi.repository.LoanRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EarlyWarningService {

    private static final String FLASK_URL = "http://localhost:5000";

    private final LoanRepository loanRepository;
    private final EarlyWarningRepository earlyWarningRepository;
    private final GroqService groqService;
    private final EmailService emailService;
    private final RestTemplate restTemplate = new RestTemplate();

    public EarlyWarningService(LoanRepository loanRepository,
            EarlyWarningRepository earlyWarningRepository,
            GroqService groqService,
            EmailService emailService) {
        this.loanRepository = loanRepository;
        this.earlyWarningRepository = earlyWarningRepository;
        this.groqService = groqService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void runNightlyScan() {
        for (Loan loan : loanRepository.findAllActiveLoans()) {
            try {
                analyzeLoan(loan);
            } catch (Exception e) {
                log.error("Erreur Early Warning pret {}: {}", loan.getId(), e.getMessage(), e);
            }
        }
    }

    public List<EarlyWarning> getRiskyWarnings() {
        return earlyWarningRepository.findRiskyWarnings();
    }

    private void analyzeLoan(Loan loan) {
        Map<String, Object> payload = Map.of(
                "loanId", loan.getId(),
                "userId", loan.getUserId(),
                "amount", loan.getPrincipalAmount(),
                "duration", loan.getDurationMonths(),
                "riskScore", loan.getRiskScore() == null ? 0 : loan.getRiskScore()
        );
        double mlScore = callScore("/predict", payload, "probability", "defaultProbability", "score");
        double markovScore = callScore("/markov", payload, "defaultedProbability", "probability", "score");
        double mcScore = callScore("/montecarlo", payload, "expectedLoss", "loss", "score");
        double combined = mlScore * 0.4 + markovScore * 0.4 + mcScore * 0.2;

        if (combined > 0.6) {
            String explanation = groqService.ask("""
                    Explique en francais pourquoi ce pret HELMA est a risque sous 30 a 90 jours.
                    Donnees: loanId=%s, userId=%s, score combine=%.2f, ML=%.2f, Markov=%.2f, MonteCarlo=%.2f,
                    montant=%s TND, duree=%s mois, riskScore=%s.
                    """.formatted(loan.getId(), loan.getUserId(), combined, mlScore, markovScore, mcScore,
                    loan.getPrincipalAmount(), loan.getDurationMonths(), loan.getRiskScore()));
            EarlyWarning warning = EarlyWarning.builder()
                    .loanId(loan.getId())
                    .userId(loan.getUserId())
                    .combinedScore(combined)
                    .mlScore(mlScore)
                    .markovScore(markovScore)
                    .mcScore(mcScore)
                    .aiExplanation(explanation)
                    .createdAt(LocalDateTime.now())
                    .status(EarlyWarningStatus.OPEN)
                    .build();
            earlyWarningRepository.save(warning);
            emailService.sendAdminEarlyWarning(loan, explanation);
        }
    }

    private double callScore(String path, Map<String, Object> payload, String... keys) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FLASK_URL + path,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            if (body == null) return 0.0;
            for (String key : keys) {
                Object value = body.get(key);
                if (value instanceof Number number) {
                    return Math.max(0.0, Math.min(1.0, number.doubleValue()));
                }
            }
        } catch (Exception e) {
            log.warn("Service Flask {} indisponible: {}", path, e.getMessage());
        }
        return 0.0;
    }
}
