package com.helma.helmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private StatistiquesDemandesDTO statistiquesDemandes;
    private StatistiquesContratsDTO statistiquesContrats;
    private StatistiquesPaiementsDTO statistiquesPaiements;
    private StatistiquesEquipementsDTO statistiquesEquipements;
    private List<EquipementPopulaireDTO> equipementsPopulaires;
    private List<RevenuMensuelDTO> revenusParMois;
}