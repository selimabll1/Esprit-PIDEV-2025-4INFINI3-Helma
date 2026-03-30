package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.projet_pi.enums.LoanStatus;
import tn.esprit.projet_pi.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {

    // 🔹 Identifiant du prêt
    private Long id;

    // 🔹 Identifiant utilisateur
    private Long userId;

    // 🔹 Type de prêt
    private LoanType loanType;

    // 🔹 Montant du prêt
    private BigDecimal principalAmount;

    // 🔹 Taux d’intérêt
    private BigDecimal interestRate;

    // 🔹 Durée en mois
    private Integer durationMonths;

    // 🔹 Mensualité calculée
    private BigDecimal monthlyPayment;

    // 🔹 Date de début
    private LocalDate startDate;

    // 🔹 Score de risque
    private Integer riskScore;

    // 🔹 Statut du prêt (PENDING, ACTIVE, CLOSED, DEFAULTED)
    private LoanStatus status;
}