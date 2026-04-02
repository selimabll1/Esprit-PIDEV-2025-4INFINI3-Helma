package com.helma.helmabackend.service;

import com.helma.helmabackend.dto.DemandeLeasingCreateRequest;
import com.helma.helmabackend.entity.DemandeLeasing;
import com.helma.helmabackend.entity.Equipement;
import com.helma.helmabackend.entity.StatutDemande;
import com.helma.helmabackend.repository.DemandeLeasingRepository;
import com.helma.helmabackend.repository.EquipementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeLeasingService {

    private final DemandeLeasingRepository demandeLeasingRepository;
    private final EquipementRepository equipementRepository;
    private final com.helma.helmabackend.repository.ContratLeasingRepository contratLeasingRepository;
    private final com.helma.helmabackend.repository.PaiementLeasingRepository paiementLeasingRepository;

    public DemandeLeasing create(DemandeLeasingCreateRequest request) {
        Equipement equipement = equipementRepository.findById(request.getEquipementId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "Equipement non trouve avec l'id: " + request.getEquipementId()));

        DemandeLeasing demande = DemandeLeasing.builder()
                .userId(request.getUserId())
                .equipement(equipement)
                .dureeMois(request.getDureeMois())
                .ageDemandeur(request.getAgeDemandeur())
                .dateDemande(LocalDate.now())
                .statut(StatutDemande.EN_ATTENTE)
                .build();

        return demandeLeasingRepository.save(demande);
    }

    public DemandeLeasing update(Long id, DemandeLeasing demande) {
        DemandeLeasing existing = findById(id);

        if (demande.getEquipement() != null && demande.getEquipement().getId() != null) {
            Equipement equipement = equipementRepository.findById(demande.getEquipement().getId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                            "Equipement non trouvé avec l'id: " + demande.getEquipement().getId()));
            existing.setEquipement(equipement);
        } else {
            existing.setEquipement(demande.getEquipement());
        }

        existing.setUserId(demande.getUserId());
        existing.setDureeMois(demande.getDureeMois());
        existing.setStatut(demande.getStatut());
        existing.setAgeDemandeur(demande.getAgeDemandeur());
        return demandeLeasingRepository.save(existing);
    }

    public void delete(Long id) {
        if (!demandeLeasingRepository.existsById(id)) {
            throw new RuntimeException("Demande non trouvée avec l'id: " + id);
        }

        contratLeasingRepository.findByDemandeId(id).ifPresent(contrat -> {
            java.util.List<com.helma.helmabackend.entity.PaiementLeasing> paiements = paiementLeasingRepository.findByContratId(contrat.getId());
            paiementLeasingRepository.deleteAll(paiements);
            contratLeasingRepository.delete(contrat);
        });

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


    @Transactional(readOnly = true)
    public Double calculateTauxAcceptation() {
        Long total = demandeLeasingRepository.count();
        if (total == 0) return 0.0;

        Long approuvees = demandeLeasingRepository.countByStatut(StatutDemande.ACCEPTEE);
        return (approuvees.doubleValue() / total.doubleValue()) * 100;
    }


    @Transactional(readOnly = true)
    public List<DemandeLeasing> getDemandesEnAttente() {
        return demandeLeasingRepository.findByStatut(StatutDemande.EN_ATTENTE);
    }
}
