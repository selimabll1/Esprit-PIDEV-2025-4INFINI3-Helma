package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIDecisionResponseDTO {

    // 🔹 Décision finale du modèle (APPROVE / REJECT / MANUAL_REVIEW)
    private String decision;

    // 🔹 Niveau de confiance du modèle (ex: "87%")
    private String confidence;

    // 🔹 Explication de la décision (important pour interprétabilité)
    private String explanation;

    // 🔹 Score de risque calculé (0 → 100)
    private Integer riskScore;

    // 🔹 Taux d’intérêt proposé en fonction du risque
    private BigDecimal interestRate;
}