package tn.esprit.helma.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour transfert de données des cartes virtuelles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCardDTO {
    private Long id;
    private Long bankAccountId;
    private String cardNumber;
    private String expiryDate;
    private String status;
    private BigDecimal paymentLimit;
    private BigDecimal monthlySpent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
