package com.helma.helmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesDemandesDTO {
    private Long totalDemandes;
    private Long demandesEnAttente;
    private Long demandesApprouvees;
    private Long demandesRejetees;
    private Double tauxApprobation;
    private Double ageMoyenDemandeurs;
    private Integer dureeMoyenneMois;
}

