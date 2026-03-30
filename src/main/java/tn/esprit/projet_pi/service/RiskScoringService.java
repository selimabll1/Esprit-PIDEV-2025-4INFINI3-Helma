package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.enums.LoanType;

import java.math.BigDecimal;

/**
 * Service de scoring de risque.
 * Implémentation à affiner selon les règles métier.
 */
@Service
public class RiskScoringService {

    /**
     * Calcule un score de risque entre 0 et 100.
     * Logique de base : montant élevé + durée longue = risque plus élevé.
     */
    public int calculateRiskScore(Long userId,
            BigDecimal principalAmount,
            Integer durationMonths,
            LoanType loanType) {
        int score = 0;

        // Montant : chaque tranche de 10 000 ajoute 10 pts (max 50)
        score += Math.min(50, principalAmount.intValue() / 10_000 * 10);

        // Durée : > 36 mois ajoute 20 pts
        if (durationMonths > 36)
            score += 20;

        // Type de prêt
        if (loanType == LoanType.BUSINESS)
            score += 20;
        else if (loanType == LoanType.STUDENT)
            score += 10;

        return Math.min(100, score);
    }
}
