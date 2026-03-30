package tn.esprit.projet_pi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatisticsDTO {

    private Long totalLoans;
    private Long activeLoans;
    private Long defaultedLoans;

    private Double par30;
    private Double par60;
    private Double par90;
}