package tn.esprit.helma.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Statistiques mensuelles des transactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatisticsDTO {
    private BigDecimal totalSpentThisMonth;
    private Map<String, BigDecimal> spendingByCategory;
    private List<BeneficiaryStatisticsDTO> topBeneficiaries;
    private Map<String, Long> transactionCountByStatus;
}
