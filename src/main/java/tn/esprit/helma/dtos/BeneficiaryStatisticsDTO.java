package tn.esprit.helma.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Statistiques agrégées pour un bénéficiaire.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryStatisticsDTO {
    private String beneficiaryName;
    private String beneficiaryRib;
    private Long transactionCount;
    private BigDecimal totalAmount;
}
