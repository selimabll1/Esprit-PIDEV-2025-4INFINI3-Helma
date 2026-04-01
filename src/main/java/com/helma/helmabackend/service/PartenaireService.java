package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.Partenaire;
import com.helma.helmabackend.repository.PartenaireRepository;
import com.helma.helmabackend.repository.EquipementRepository;
import com.helma.helmabackend.repository.DemandeLeasingRepository;
import com.helma.helmabackend.repository.ContratLeasingRepository;
import com.helma.helmabackend.repository.PaiementLeasingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;
    private final EquipementRepository equipementRepository;
    private final DemandeLeasingRepository demandeLeasingRepository;
    private final ContratLeasingRepository contratLeasingRepository;
    private final PaiementLeasingRepository paiementLeasingRepository;

    public Partenaire create(Partenaire partenaire) {
        return partenaireRepository.save(partenaire);
    }

    public Partenaire update(Long id, Partenaire partenaire) {
        Partenaire existing = findById(id);
        existing.setNom(partenaire.getNom());
        existing.setType(partenaire.getType());
        existing.setContact(partenaire.getContact());
        existing.setActif(partenaire.getActif());
        return partenaireRepository.save(existing);
    }

    public void delete(Long id) {
        if (!partenaireRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire non trouvé avec l'id: " + id);
        }

        List<com.helma.helmabackend.entity.Equipement> equipements = equipementRepository.findByPartenaireId(id);
        for (com.helma.helmabackend.entity.Equipement equipement : equipements) {
            List<com.helma.helmabackend.entity.DemandeLeasing> demandes = demandeLeasingRepository.findByEquipementId(equipement.getId());
            for (com.helma.helmabackend.entity.DemandeLeasing demande : demandes) {
                contratLeasingRepository.findByDemandeId(demande.getId()).ifPresent(contrat -> {
                    List<com.helma.helmabackend.entity.PaiementLeasing> paiements = paiementLeasingRepository.findByContratId(contrat.getId());
                    paiementLeasingRepository.deleteAll(paiements);
                    contratLeasingRepository.delete(contrat);
                });
            }
            demandeLeasingRepository.deleteAll(demandes);
        }
        equipementRepository.deleteAll(equipements);

        partenaireRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Partenaire findById(Long id) {
        return partenaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findAll() {
        return partenaireRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findByActif(Boolean actif) {
        return partenaireRepository.findByActif(actif);
    }

    @Transactional(readOnly = true)
    public List<Partenaire> findByType(String type) {
        return partenaireRepository.findByType(type);
    }

    public Partenaire updateActif(Long id, Boolean actif) {
        Partenaire partenaire = findById(id);
        partenaire.setActif(actif);
        return partenaireRepository.save(partenaire);
    }
}
