package tn.esprit.helma.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour transfert de données des transactions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private Long id;
    private Long bankAccountId;
    private String beneficiaryName;
    private String beneficiaryRib;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String status;
    private String periodicity;
    private LocalDateTime scheduledDate;
    private LocalDateTime nextExecutionDate;
    private LocalDateTime lastExecutionDate;
    private Integer riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRate;
    private String targetCurrency;
    private Boolean pinRequired;
}
