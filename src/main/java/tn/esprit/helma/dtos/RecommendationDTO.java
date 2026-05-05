package tn.esprit.helma.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recommandation métier générée à partir de l'historique transactionnel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private String type;
    private String priority;
    private String title;
    private String message;
    private String action;
    private String value;
}
