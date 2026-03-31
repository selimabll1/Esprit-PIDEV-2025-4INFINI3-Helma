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
public class StatistiquesPaiementsDTO {
    private Long totalPaiements;
    private Long paiementsPayes;
    private Long paiementsEnAttente;
    private Long paiementsEnRetard;
    private Double tauxPaiement;
    private BigDecimal montantTotalPaye;
    private BigDecimal montantEnAttente;
}

