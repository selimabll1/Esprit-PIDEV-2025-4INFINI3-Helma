package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.dto.request.LoanSimulationDTO;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de simulation de prêt.
 * Calcule l'échéancier théorique sans persister en base.
 */
@Service
public class LoanSimulationService {

    /**
     * Simule un prêt et retourne les détails de la simulation :
     * mensualité, coût total, intérêts totaux, et échéancier théorique.
     */
    public Map<String, Object> simulate(LoanSimulationDTO dto) {
        BigDecimal principal = dto.getPrincipalAmount();
        int n = dto.getDurationMonths();

        // ── Taux par défaut microfinance ─────────────────────────────────
        // Si le taux est absent ou irréaliste (< 12 %), on applique 20 %.
        BigDecimal annualRate = dto.getInterestRate();
        if (annualRate == null || annualRate.compareTo(new BigDecimal("12.0")) < 0) {
            annualRate = new BigDecimal("20.0");
        }

        BigDecimal monthlyPayment;

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            monthlyPayment = principal.divide(BigDecimal.valueOf(n), 3, RoundingMode.HALF_UP);
        } else {
            BigDecimal r = annualRate
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            BigDecimal onePlusR = BigDecimal.ONE.add(r);
            BigDecimal pow = onePlusR.pow(n, new MathContext(15, RoundingMode.HALF_UP));
            BigDecimal numerator = principal.multiply(r).multiply(pow);
            BigDecimal denominator = pow.subtract(BigDecimal.ONE);
            monthlyPayment = numerator.divide(denominator, 3, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = monthlyPayment.multiply(BigDecimal.valueOf(n));
        BigDecimal totalInterest = totalCost.subtract(principal);

        // Génération de l'échéancier simulé (non persisté)
        List<Map<String, Object>> schedule = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < n; i++) {
            Map<String, Object> installment = new LinkedHashMap<>();
            installment.put("installmentNumber", i + 1);
            installment.put("dueDate", today.plusMonths(i + 1).toString());
            installment.put("expectedAmount", monthlyPayment);
            schedule.add(installment);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principalAmount", principal);
        result.put("interestRate", annualRate);
        result.put("durationMonths", n);
        result.put("monthlyPayment", monthlyPayment);
        result.put("totalCost", totalCost);
        result.put("totalInterest", totalInterest);
        result.put("schedule", schedule);

        return result;
    }
}
