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
public class StatistiquesContratsDTO {
    private Long totalContrats;
    private Long contratsActifs;
    private Long contratsTermines;
    private Long contratsResilis;
    private BigDecimal revenuMensuelTotal;
    private BigDecimal revenuAnnuelEstime;
    private BigDecimal loyerMoyenMensuel;
}

