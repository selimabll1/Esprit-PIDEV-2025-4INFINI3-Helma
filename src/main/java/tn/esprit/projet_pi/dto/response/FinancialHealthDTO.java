package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthDTO {
    private Long userId;
    private Integer helmaScore;
    private String level;
    private BigDecimal maxLoanAmount;
    private boolean preferentialRate;
    private Details details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Details {
        private Integer repaymentHistory;
        private Integer regularity;
        private Integer debtRatio;
        private Integer seniority;
        private Integer loanType;
    }
}
