package tn.esprit.helma.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Requete de creation d'une transaction.
 * 
 * ⚡ VALIDATION INTELLIGENTE SELON PERIODICITY:
 * 
 * 1️⃣ NOW (Immédiate):
 *    - Date automatique: createdAt = maintenant
 *    - scheduledDate: IGNORÉ (pas nécessaire)
 *    - nextExecutionDate: IGNORÉ (pas nécessaire)
 *    ✅ Exemple: { "periodicity": "NOW" }
 * 
 * 2️⃣ SCHEDULED (Programmée):
 *    - Date requise: scheduledDate (date d'exécution)
 *    - nextExecutionDate: IGNORÉ (pas nécessaire)
 *    ⚠️ Si scheduledDate absent → ERREUR 400
 *    ✅ Exemple: { "periodicity": "SCHEDULED", "scheduledDate": "2026-02-22T10:00:00" }
 * 
 * 3️⃣ PERMANENT (Permanente):
 *    - Date requise: nextExecutionDate (prochaine exécution)
 *    - scheduledDate: IGNORÉ (pas nécessaire)
 *    ⚠️ Si nextExecutionDate absent → défaut +30 jours
 *    ✅ Exemple: { "periodicity": "PERMANENT", "nextExecutionDate": "2026-03-21T00:00:00" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String beneficiaryName;

    @Size(max = 27)
    private String beneficiaryRib;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    @Size(max = 50)
    private String category;

    @Size(max = 500)
    private String description;

    /**
     * Périodicité de la transaction (NOW, SCHEDULED, PERMANENT)
     * Par défaut: NOW (immédiat)
     */
    @Builder.Default
    private TransactionPeriodicity periodicity = TransactionPeriodicity.NOW;

    /**
     * Date programmée (REQUISE pour SCHEDULED, IGNORÉE pour NOW et PERMANENT)
     * 
     * ⚠️ OBLIGATOIRE si periodicity = "SCHEDULED"
     * ✅ VÉRIFICATION: Service valident automatiquement
     */
    private LocalDateTime scheduledDate;

    /**
     * Date de prochaine exécution (REQUISE pour PERMANENT, IGNORÉE pour NOW et SCHEDULED)
     *
     * ⚠️ OPTIONNEL pour PERMANENT (défaut: +30 jours à partir de maintenant)
     * ✅ VÉRIFICATION: Service valident automatiquement
     */
    private LocalDateTime nextExecutionDate;

    /**
     * Devise cible du bénéficiaire pour conversion automatique (optionnel)
     * Ex: "USD" si le bénéficiaire souhaite recevoir en dollars
     */
    @Size(max = 3)
    private String beneficiaryCurrency;
}