package tn.esprit.helma.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour transfert de données du compte bancaire
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountDTO {
    private Long id;
    private Long userId;
    private String rib;
    private BigDecimal balance;
    private String currency;
    private String accountType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
