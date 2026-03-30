package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.enums.LoanType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service de calcul du taux d'intérêt — taux microfinance réalistes.
 */
@Service
public class InterestRateService {

    /**
     * Retourne un taux d'intérêt annuel (en %) adapté à la microfinance.
     *
     * @param userId           identifiant de l'utilisateur
     * @param principalAmount  montant du prêt
     * @param loanType         type de prêt
     * @param riskScore        score de risque (0–100)
     * @param overdueCount     nombre d'échéances en retard (historique)
     * @param defaultCount     nombre de défauts de paiement passés
     * @param activeLoansCount nombre de prêts actifs actuels
     */
    public BigDecimal calculateRate(Long userId,
            BigDecimal principalAmount,
            LoanType loanType,
            int riskScore,
            int overdueCount,
            int defaultCount,
            int activeLoansCount) {

        // ── Taux de base selon le type de prêt ──────────────────────────
        double baseRate;
        switch (loanType) {
            case STUDENT:
                baseRate = 15.0;
                break;
            case PERSONAL:
                baseRate = 20.0;
                break;
            case BUSINESS:
            default:
                baseRate = 28.0;
                break;
        }

        // ── Ajustements ─────────────────────────────────────────────────

        // Bonus fidélité : pas de défaut et au moins un prêt actif
        if (defaultCount == 0 && activeLoansCount > 0) {
            baseRate -= 2.0;
        }

        // Bonus petits montants (< 1 000)
        if (principalAmount.compareTo(new BigDecimal("1000")) < 0) {
            baseRate -= 1.0;
        }

        // Pénalité par échéance en retard
        baseRate += overdueCount * 3.0;

        // Majoration supplémentaire pour les prêts professionnels
        if (loanType == LoanType.BUSINESS) {
            baseRate += 4.0;
        }

        // Majoration selon le score de risque
        if (riskScore > 60)
            baseRate += 5.0;
        if (riskScore > 80)
            baseRate += 5.0;

        // ── Limites finales ──────────────────────────────────────────────
        if (baseRate < 12.0)
            baseRate = 12.0;
        if (baseRate > 40.0)
            baseRate = 40.0;

        return BigDecimal.valueOf(baseRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Surcharge simplifiée pour les appels sans historique
     * (riskScore fourni, pas d'historique disponible).
     */
    public BigDecimal calculateRate(Long userId,
            BigDecimal principalAmount,
            LoanType loanType) {
        return calculateRate(userId, principalAmount, loanType, 0, 0, 0, 0);
    }
}
