package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.Equipement;
import com.helma.helmabackend.repository.EquipementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipementService {

    private final EquipementRepository equipementRepository;
    private final com.helma.helmabackend.repository.PartenaireRepository partenaireRepository;
    private final com.helma.helmabackend.repository.DemandeLeasingRepository demandeLeasingRepository;
    private final com.helma.helmabackend.repository.ContratLeasingRepository contratLeasingRepository;
    private final com.helma.helmabackend.repository.PaiementLeasingRepository paiementLeasingRepository;

    public Equipement create(Equipement equipement) {
        if (equipement.getPartenaire() != null && equipement.getPartenaire().getId() != null) {
            com.helma.helmabackend.entity.Partenaire partenaire = partenaireRepository.findById(equipement.getPartenaire().getId())
                    .orElseThrow(() -> new RuntimeException("Partenaire non trouvé avec l'id: " + equipement.getPartenaire().getId()));
            equipement.setPartenaire(partenaire);
        }
        return equipementRepository.save(equipement);
    }

    public Equipement update(Long id, Equipement equipement) {
        Equipement existing = findById(id);

        if (equipement.getPartenaire() != null && equipement.getPartenaire().getId() != null) {
            com.helma.helmabackend.entity.Partenaire partenaire = partenaireRepository.findById(equipement.getPartenaire().getId())
                    .orElseThrow(() -> new RuntimeException("Partenaire non trouvé avec l'id: " + equipement.getPartenaire().getId()));
            existing.setPartenaire(partenaire);
        } else {
            existing.setPartenaire(equipement.getPartenaire());
        }

        existing.setNom(equipement.getNom());
        existing.setCategorie(equipement.getCategorie());
        existing.setValeur(equipement.getValeur());
        existing.setDisponible(equipement.getDisponible());
        existing.setPartenaire(equipement.getPartenaire());
        return equipementRepository.save(existing);
    }

    public void delete(Long id) {
        if (!equipementRepository.existsById(id)) {
            throw new RuntimeException("Equipement non trouvé avec l'id: " + id);
        }

        // Supprimer toutes les demandes associées, leurs contrats, et paiements (sans toucher au partenaire)
        List<com.helma.helmabackend.entity.DemandeLeasing> demandes = demandeLeasingRepository.findByEquipementId(id);
        for (com.helma.helmabackend.entity.DemandeLeasing demande : demandes) {
            contratLeasingRepository.findByDemandeId(demande.getId()).ifPresent(contrat -> {
                List<com.helma.helmabackend.entity.PaiementLeasing> paiements = paiementLeasingRepository.findByContratId(contrat.getId());
                paiementLeasingRepository.deleteAll(paiements);
                contratLeasingRepository.delete(contrat);
            });
        }
        demandeLeasingRepository.deleteAll(demandes);

        equipementRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Equipement findById(Long id) {
        return equipementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipement non trouvé avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Equipement> findAll() {
        return equipementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Equipement> findByDisponible(Boolean disponible) {
        return equipementRepository.findByDisponible(disponible);
    }

    @Transactional(readOnly = true)
    public List<Equipement> findByCategorie(String categorie) {
        return equipementRepository.findByCategorie(categorie);
    }

    @Transactional(readOnly = true)
    public List<Equipement> findByPartenaireId(Long partenaireId) {
        return equipementRepository.findByPartenaireId(partenaireId);
    }

    public Equipement updateDisponibilite(Long id, Boolean disponible) {
        Equipement equipement = findById(id);
        equipement.setDisponible(disponible);
        return equipementRepository.save(equipement);
    }


    @Transactional(readOnly = true)
    public Double calculateTauxUtilisation() {
        Long total = equipementRepository.count();
        if (total == 0) return 0.0;

        Long enLocation = equipementRepository.countByDisponible(false);
        return (enLocation.doubleValue() / total.doubleValue()) * 100;
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateValeurTotale() {
        BigDecimal valeur = equipementRepository.calculateValeurTotale();
        return valeur != null ? valeur : BigDecimal.ZERO;
    }


    @Transactional(readOnly = true)
    public List<Equipement> getEquipementsDisponibles() {
        return equipementRepository.findByDisponible(true);
    }
}
