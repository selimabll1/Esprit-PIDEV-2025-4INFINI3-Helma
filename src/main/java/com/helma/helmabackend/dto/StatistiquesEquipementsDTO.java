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
public class StatistiquesEquipementsDTO {
    private Long totalEquipements;
    private Long equipementsDisponibles;
    private Long equipementsEnLocation;
    private Double tauxUtilisation;
    private BigDecimal valeurTotaleEquipements;
    private BigDecimal valeurMoyenne;
}

