package com.helma.helmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioMetricsDTO {
    private BigDecimal encoursTotal;
    private Double tauxRecouvrement;
    private Double rendementAnnuel;
    private BigDecimal arpc;
}
