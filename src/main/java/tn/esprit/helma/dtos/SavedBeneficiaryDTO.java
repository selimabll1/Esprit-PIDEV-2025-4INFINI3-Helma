package tn.esprit.helma.dtos;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO pour transfert de données des bénéficiaires
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedBeneficiaryDTO {
    private Long id;
    private Long userId;
    private String beneficiaryName;
    private String beneficiaryRib;
    private String alias;
    private Integer transferCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
