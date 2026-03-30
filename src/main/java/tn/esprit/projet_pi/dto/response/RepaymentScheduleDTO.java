package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleDTO {

    private Long id;
    private Long loanId;
    private Integer installmentNumber;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private String status;
}