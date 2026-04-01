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
public class AdvancedFinancialDTO {
    private BigDecimal chiffreAffaires;
    private BigDecimal totalImpayes;
    private Double tauxDefaut;
    private Double roiFlotte;
}
