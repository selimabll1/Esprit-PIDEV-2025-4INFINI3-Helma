package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.DemandeLeasing;
import com.helma.helmabackend.entity.StatutDemande;
import com.helma.helmabackend.repository.DemandeLeasingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeLeasingService {

    private final DemandeLeasingRepository demandeLeasingRepository;

    public DemandeLeasing create(DemandeLeasing demande) {
        return demandeLeasingRepository.save(demande);
    }

    public DemandeLeasing update(Long id, DemandeLeasing demande) {
        DemandeLeasing existing = findById(id);
        existing.setUserId(demande.getUserId());
        existing.setEquipement(demande.getEquipement());
        existing.setDureeMois(demande.getDureeMois());
        existing.setStatut(demande.getStatut());
        existing.setAgeDemandeur(demande.getAgeDemandeur());
        return demandeLeasingRepository.save(existing);
    }

    public void delete(Long id) {
        demandeLeasingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DemandeLeasing findById(Long id) {
        return demandeLeasingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<DemandeLeasing> findAll() {
        return demandeLeasingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DemandeLeasing> findByUserId(Long userId) {
        return demandeLeasingRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<DemandeLeasing> findByStatut(StatutDemande statut) {
        return demandeLeasingRepository.findByStatut(statut);
    }

    @Transactional(readOnly = true)
    public List<DemandeLeasing> findByEquipementId(Long equipementId) {
        return demandeLeasingRepository.findByEquipementId(equipementId);
    }

    public DemandeLeasing updateStatut(Long id, StatutDemande statut) {
        DemandeLeasing demande = findById(id);
        demande.setStatut(statut);
        return demandeLeasingRepository.save(demande);
    }

    /**
     * Calcule le taux d'acceptation global des demandes
     */
    @Transactional(readOnly = true)
    public Double calculateTauxAcceptation() {
        Long total = demandeLeasingRepository.count();
        if (total == 0) return 0.0;

        Long approuvees = demandeLeasingRepository.countByStatut(StatutDemande.ACCEPTEE);
        return (approuvees.doubleValue() / total.doubleValue()) * 100;
    }

    /**
     * Récupère les demandes nécessitant une action (en attente)
     */
    @Transactional(readOnly = true)
    public List<DemandeLeasing> getDemandesEnAttente() {
        return demandeLeasingRepository.findByStatut(StatutDemande.EN_ATTENTE);
    }
}
