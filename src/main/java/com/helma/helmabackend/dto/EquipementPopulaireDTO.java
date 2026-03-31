package com.helma.helmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipementPopulaireDTO {
    private Long equipementId;
    private String nomEquipement;
    private String categorie;
    private Long nombreDemandes;
}

