package com.helma.helmabackend.service;

import com.helma.helmabackend.dto.*;
import com.helma.helmabackend.entity.StatutContrat;
import com.helma.helmabackend.entity.StatutDemande;
import com.helma.helmabackend.entity.StatutPaiement;
import com.helma.helmabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatistiquesService {

    private final DemandeLeasingRepository demandeLeasingRepository;
    private final ContratLeasingRepository contratLeasingRepository;
    private final PaiementLeasingRepository paiementLeasingRepository;
    private final EquipementRepository equipementRepository;


    public StatistiquesDemandesDTO getStatistiquesDemandes() {
        Long total = demandeLeasingRepository.count();
        Long enAttente = demandeLeasingRepository.countByStatut(StatutDemande.EN_ATTENTE);
        Long approuvees = demandeLeasingRepository.countByStatut(StatutDemande.ACCEPTEE);
        Long rejetees = demandeLeasingRepository.countByStatut(StatutDemande.REFUSEE);

        Double tauxApprobation = total > 0 
            ? (approuvees.doubleValue() / total.doubleValue()) * 100 
            : 0.0;
        
        Double ageMoyen = demandeLeasingRepository.findAgeMoyenDemandeurs();
        Double dureeMoyenne = demandeLeasingRepository.findDureeMoyenneMois();

        return StatistiquesDemandesDTO.builder()
                .totalDemandes(total)
                .demandesEnAttente(enAttente)
                .demandesApprouvees(approuvees)
                .demandesRejetees(rejetees)
                .tauxApprobation(Math.round(tauxApprobation * 100.0) / 100.0)
                .ageMoyenDemandeurs(ageMoyen != null ? Math.round(ageMoyen * 100.0) / 100.0 : 0.0)
                .dureeMoyenneMois(dureeMoyenne != null ? dureeMoyenne.intValue() : 0)
                .build();
    }


    public StatistiquesContratsDTO getStatistiquesContrats() {
        Long total = contratLeasingRepository.count();
        Long actifs = contratLeasingRepository.countByStatut(StatutContrat.ACTIF);
        Long termines = contratLeasingRepository.countByStatut(StatutContrat.TERMINE);
        Long resilis = contratLeasingRepository.countByStatut(StatutContrat.SUSPENDU);

        BigDecimal revenuMensuel = contratLeasingRepository.calculateRevenuMensuelTotal();
        if (revenuMensuel == null) revenuMensuel = BigDecimal.ZERO;
        
        BigDecimal revenuAnnuel = revenuMensuel.multiply(BigDecimal.valueOf(12));
        
        BigDecimal loyerMoyen = contratLeasingRepository.calculateLoyerMoyenMensuel();
        if (loyerMoyen == null) loyerMoyen = BigDecimal.ZERO;

        return StatistiquesContratsDTO.builder()
                .totalContrats(total)
                .contratsActifs(actifs)
                .contratsTermines(termines)
                .contratsResilis(resilis)
                .revenuMensuelTotal(revenuMensuel.setScale(2, RoundingMode.HALF_UP))
                .revenuAnnuelEstime(revenuAnnuel.setScale(2, RoundingMode.HALF_UP))
                .loyerMoyenMensuel(loyerMoyen.setScale(2, RoundingMode.HALF_UP))
                .build();
    }


    public StatistiquesPaiementsDTO getStatistiquesPaiements() {
        Long total = paiementLeasingRepository.count();
        Long payes = paiementLeasingRepository.countByStatutPaiement(StatutPaiement.PAYE);
        Long enAttente = paiementLeasingRepository.countByStatutPaiement(StatutPaiement.EN_ATTENTE);
        Long enRetard = paiementLeasingRepository.countByStatutPaiement(StatutPaiement.EN_RETARD);
        
        Double tauxPaiement = total > 0 
            ? (payes.doubleValue() / total.doubleValue()) * 100 
            : 0.0;
        
        BigDecimal montantPaye = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.PAYE);
        if (montantPaye == null) montantPaye = BigDecimal.ZERO;
        
        BigDecimal montantEnAttente = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_ATTENTE);
        if (montantEnAttente == null) montantEnAttente = BigDecimal.ZERO;

        return StatistiquesPaiementsDTO.builder()
                .totalPaiements(total)
                .paiementsPayes(payes)
                .paiementsEnAttente(enAttente)
                .paiementsEnRetard(enRetard)
                .tauxPaiement(Math.round(tauxPaiement * 100.0) / 100.0)
                .montantTotalPaye(montantPaye.setScale(2, RoundingMode.HALF_UP))
                .montantEnAttente(montantEnAttente.setScale(2, RoundingMode.HALF_UP))
                .build();
    }


    public StatistiquesEquipementsDTO getStatistiquesEquipements() {
        Long total = equipementRepository.count();
        Long disponibles = equipementRepository.countByDisponible(true);
        Long enLocation = equipementRepository.countByDisponible(false);
        
        Double tauxUtilisation = total > 0 
            ? (enLocation.doubleValue() / total.doubleValue()) * 100 
            : 0.0;
        
        BigDecimal valeurTotale = equipementRepository.calculateValeurTotale();
        if (valeurTotale == null) valeurTotale = BigDecimal.ZERO;
        
        BigDecimal valeurMoyenne = equipementRepository.calculateValeurMoyenne();
        if (valeurMoyenne == null) valeurMoyenne = BigDecimal.ZERO;

        return StatistiquesEquipementsDTO.builder()
                .totalEquipements(total)
                .equipementsDisponibles(disponibles)
                .equipementsEnLocation(enLocation)
                .tauxUtilisation(Math.round(tauxUtilisation * 100.0) / 100.0)
                .valeurTotaleEquipements(valeurTotale.setScale(2, RoundingMode.HALF_UP))
                .valeurMoyenne(valeurMoyenne.setScale(2, RoundingMode.HALF_UP))
                .build();
    }


    public List<EquipementPopulaireDTO> getEquipementsLesPlusDemandes(int limit) {
        List<Object[]> results = demandeLeasingRepository.findEquipementsLesPlusDemandesRaw();
        List<EquipementPopulaireDTO> equipements = new ArrayList<>();
        
        int count = 0;
        for (Object[] result : results) {
            if (count >= limit) break;
            
            equipements.add(EquipementPopulaireDTO.builder()
                    .equipementId((Long) result[0])
                    .nomEquipement((String) result[1])
                    .categorie((String) result[2])
                    .nombreDemandes((Long) result[3])
                    .build());
            count++;
        }
        
        return equipements;
    }


    public List<RevenuMensuelDTO> getRevenusParMois() {
        List<Object[]> results = paiementLeasingRepository.findRevenusDetaillesParMoisRaw();
        List<RevenuMensuelDTO> revenus = new ArrayList<>();
        
        for (Object[] result : results) {
            BigDecimal revenuPaye = (BigDecimal) result[1];
            BigDecimal revenuAttendu = (BigDecimal) result[2];
            
            if (revenuPaye == null) revenuPaye = BigDecimal.ZERO;
            if (revenuAttendu == null) revenuAttendu = BigDecimal.ZERO;
            
            revenus.add(RevenuMensuelDTO.builder()
                    .mois((String) result[0])
                    .revenuPaye(revenuPaye.setScale(2, RoundingMode.HALF_UP))
                    .revenuAttendu(revenuAttendu.setScale(2, RoundingMode.HALF_UP))
                    .nombrePaiements((Long) result[3])
                    .build());
        }
        
        return revenus;
    }


    public BigDecimal calculatePrevisionRevenus(int nombreMois) {
        BigDecimal revenuMensuelActuel = contratLeasingRepository.calculateRevenuMensuelTotal();
        if (revenuMensuelActuel == null) revenuMensuelActuel = BigDecimal.ZERO;
        
        return revenuMensuelActuel
                .multiply(BigDecimal.valueOf(nombreMois))
                .setScale(2, RoundingMode.HALF_UP);
    }


    public AdvancedFinancialDTO getAdvancedFinancialMetrics() {

        BigDecimal chiffreAffaires = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.PAYE);
        if (chiffreAffaires == null) chiffreAffaires = BigDecimal.ZERO;


        BigDecimal impayes = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_RETARD);
        if (impayes == null) impayes = BigDecimal.ZERO;

        BigDecimal enAttente = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_ATTENTE);
        if (enAttente == null) enAttente = BigDecimal.ZERO;


        BigDecimal totalAttendu = chiffreAffaires.add(impayes).add(enAttente);
        Double tauxDefaut = 0.0;
        if (totalAttendu.compareTo(BigDecimal.ZERO) > 0) {
            tauxDefaut = impayes.divide(totalAttendu, 4, RoundingMode.HALF_UP)
                               .multiply(BigDecimal.valueOf(100)).doubleValue();
        }


        BigDecimal valeurFlotte = equipementRepository.calculateValeurTotale();
        if (valeurFlotte == null) valeurFlotte = BigDecimal.ZERO;

        Double roiFlotte = 0.0;
        if (valeurFlotte.compareTo(BigDecimal.ZERO) > 0) {
            roiFlotte = chiffreAffaires.divide(valeurFlotte, 4, RoundingMode.HALF_UP)
                                      .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        return AdvancedFinancialDTO.builder()
                .chiffreAffaires(chiffreAffaires.setScale(2, RoundingMode.HALF_UP))
                .totalImpayes(impayes.setScale(2, RoundingMode.HALF_UP))
                .tauxDefaut(Math.round(tauxDefaut * 100.0) / 100.0)
                .roiFlotte(Math.round(roiFlotte * 100.0) / 100.0)
                .build();
    }


    public PortfolioMetricsDTO getPortfolioMetrics() {

        BigDecimal encoursTotal = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_ATTENTE);
        if (encoursTotal == null) encoursTotal = BigDecimal.ZERO;


        BigDecimal paye = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.PAYE);
        if (paye == null) paye = BigDecimal.ZERO;

        BigDecimal enRetard = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_RETARD);
        if (enRetard == null) enRetard = BigDecimal.ZERO;

        BigDecimal totalDu = paye.add(enRetard);
        Double tauxRecouvrement = 0.0;
        if (totalDu.compareTo(BigDecimal.ZERO) > 0) {

            tauxRecouvrement = paye.divide(totalDu, 4, RoundingMode.HALF_UP)
                                  .multiply(BigDecimal.valueOf(100)).doubleValue();
        }


        BigDecimal revenuMensuel = contratLeasingRepository.calculateRevenuMensuelTotal();
        if (revenuMensuel == null) revenuMensuel = BigDecimal.ZERO;
        BigDecimal revenuAnnuel = revenuMensuel.multiply(BigDecimal.valueOf(12));

        BigDecimal valeurFlotte = equipementRepository.calculateValeurTotale();
        if (valeurFlotte == null) valeurFlotte = BigDecimal.ZERO;

        Double rendementAnnuel = 0.0;
        if (valeurFlotte.compareTo(BigDecimal.ZERO) > 0) {
            rendementAnnuel = revenuAnnuel.divide(valeurFlotte, 4, RoundingMode.HALF_UP)
                                          .multiply(BigDecimal.valueOf(100)).doubleValue();
        }


        Long totalContrats = contratLeasingRepository.countByStatut(StatutContrat.ACTIF);
        BigDecimal arpc = BigDecimal.ZERO;
        if (totalContrats > 0) {
            arpc = revenuAnnuel.divide(BigDecimal.valueOf(totalContrats), 2, RoundingMode.HALF_UP);
        }

        return PortfolioMetricsDTO.builder()
                .encoursTotal(encoursTotal.setScale(2, RoundingMode.HALF_UP))
                .tauxRecouvrement(Math.round(tauxRecouvrement * 100.0) / 100.0)
                .rendementAnnuel(Math.round(rendementAnnuel * 100.0) / 100.0)
                .arpc(arpc)
                .build();
    }
}
