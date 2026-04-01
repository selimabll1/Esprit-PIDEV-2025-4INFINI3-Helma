package com.helma.helmabackend.controller;


import com.helma.helmabackend.dto.*;
import com.helma.helmabackend.service.StatistiquesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatistiquesController {

    private final StatistiquesService statistiquesService;

    /**
     * GET /api/statistiques/demandes
     * Récupère les statistiques globales des demandes de leasing
     */
    @GetMapping("/demandes")
    public ResponseEntity<StatistiquesDemandesDTO> getStatistiquesDemandes() {
        StatistiquesDemandesDTO stats = statistiquesService.getStatistiquesDemandes();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/statistiques/contrats
     * Récupère les statistiques des contrats de leasing
     */
    @GetMapping("/contrats")
    public ResponseEntity<StatistiquesContratsDTO> getStatistiquesContrats() {
        StatistiquesContratsDTO stats = statistiquesService.getStatistiquesContrats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/statistiques/paiements
     * Récupère les statistiques des paiements
     */
    @GetMapping("/paiements")
    public ResponseEntity<StatistiquesPaiementsDTO> getStatistiquesPaiements() {
        StatistiquesPaiementsDTO stats = statistiquesService.getStatistiquesPaiements();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/statistiques/equipements
     * Récupère les statistiques des équipements
     */
    @GetMapping("/equipements")
    public ResponseEntity<StatistiquesEquipementsDTO> getStatistiquesEquipements() {
        StatistiquesEquipementsDTO stats = statistiquesService.getStatistiquesEquipements();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/statistiques/equipements/populaires?limit=10
     * Récupère les équipements les plus demandés
     */
    @GetMapping("/equipements/populaires")
    public ResponseEntity<List<EquipementPopulaireDTO>> getEquipementsLesPlusDemandes(
            @RequestParam(defaultValue = "10") int limit) {
        List<EquipementPopulaireDTO> equipements = statistiquesService.getEquipementsLesPlusDemandes(limit);
        return ResponseEntity.ok(equipements);
    }

    /**
     * GET /api/statistiques/revenus/mensuel
     * Récupère les revenus détaillés par mois
     */
    @GetMapping("/revenus/mensuel")
    public ResponseEntity<List<RevenuMensuelDTO>> getRevenusParMois() {
        List<RevenuMensuelDTO> revenus = statistiquesService.getRevenusParMois();
        return ResponseEntity.ok(revenus);
    }

    /**
     * GET /api/statistiques/revenus/prevision?mois=6
     * Calcule la prévision de revenus pour les N prochains mois
     */
    @GetMapping("/revenus/prevision")
    public ResponseEntity<BigDecimal> getPrevisionRevenus(
            @RequestParam(defaultValue = "6") int mois) {
        BigDecimal prevision = statistiquesService.calculatePrevisionRevenus(mois);
        return ResponseEntity.ok(prevision);
    }

    /**
     * GET /api/statistiques/finance/advanced
     * Récupère les calculs financiers avancés (ROI, Impayés, etc.)
     */
    @GetMapping("/finance/advanced")
    public ResponseEntity<AdvancedFinancialDTO> getAdvancedFinancialMetrics() {
        AdvancedFinancialDTO metrics = statistiquesService.getAdvancedFinancialMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/statistiques/finance/portfolio
     * Récupère les indicateurs de rendement et risque du portefeuille
     */
    @GetMapping("/finance/portfolio")
    public ResponseEntity<PortfolioMetricsDTO> getPortfolioMetrics() {
        PortfolioMetricsDTO metrics = statistiquesService.getPortfolioMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/statistiques/dashboard
     * Récupère toutes les statistiques principales pour le tableau de bord
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        DashboardDTO dashboard = DashboardDTO.builder()
                .statistiquesDemandes(statistiquesService.getStatistiquesDemandes())
                .statistiquesContrats(statistiquesService.getStatistiquesContrats())
                .statistiquesPaiements(statistiquesService.getStatistiquesPaiements())
                .statistiquesEquipements(statistiquesService.getStatistiquesEquipements())
                .equipementsPopulaires(statistiquesService.getEquipementsLesPlusDemandes(5))
                .revenusParMois(statistiquesService.getRevenusParMois())
                .build();
        return ResponseEntity.ok(dashboard);
    }
}
