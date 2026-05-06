package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiAgentDecisionDTO {
    private Long loanId;
    private Integer originalRiskScore;
    private String creditScoringAnalysis;
    private Integer adjustedScore;
    private Boolean criticalFlag;
    private String fraudDetectionAnalysis;
    private String fraudRisk;
    private String marketContextAnalysis;
    private String marketContext;
    private String finalDecisionAnalysis;
    private String finalDecision;
    private String explanation;
}
