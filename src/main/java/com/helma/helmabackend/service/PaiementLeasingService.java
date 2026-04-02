package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.PaiementLeasing;
import com.helma.helmabackend.entity.StatutPaiement;
import com.helma.helmabackend.repository.PaiementLeasingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementLeasingService {

    private final PaiementLeasingRepository paiementLeasingRepository;
    private final com.helma.helmabackend.repository.ContratLeasingRepository contratLeasingRepository;

    public PaiementLeasing create(PaiementLeasing paiement) {
        if (paiement.getContrat() == null || paiement.getContrat().getId() == null) {
            throw new IllegalArgumentException("Le paiement doit être rattaché à un contrat valide.");
        }
        com.helma.helmabackend.entity.ContratLeasing contrat = contratLeasingRepository.findById(paiement.getContrat().getId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + paiement.getContrat().getId()));
        paiement.setContrat(contrat);
        return paiementLeasingRepository.save(paiement);
    }

    public PaiementLeasing update(Long id, PaiementLeasing paiement) {
        PaiementLeasing existing = findById(id);

        if (paiement.getContrat() != null && paiement.getContrat().getId() != null) {
            com.helma.helmabackend.entity.ContratLeasing contrat = contratLeasingRepository.findById(paiement.getContrat().getId())
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + paiement.getContrat().getId()));
            existing.setContrat(contrat);
        }

        existing.setMois(paiement.getMois());
        existing.setMontant(paiement.getMontant());
        existing.setStatutPaiement(paiement.getStatutPaiement());
        existing.setDatePaiement(paiement.getDatePaiement());
        return paiementLeasingRepository.save(existing);
    }

    public void delete(Long id) {
        if (!paiementLeasingRepository.existsById(id)) {
            throw new RuntimeException("Paiement non trouvé avec l'id: " + id);
        }
        paiementLeasingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PaiementLeasing findById(Long id) {
        return paiementLeasingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<PaiementLeasing> findAll() {
        return paiementLeasingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PaiementLeasing> findByContratId(Long contratId) {
        return paiementLeasingRepository.findByContratId(contratId);
    }

    @Transactional(readOnly = true)
    public List<PaiementLeasing> findByStatutPaiement(StatutPaiement statutPaiement) {
        return paiementLeasingRepository.findByStatutPaiement(statutPaiement);
    }

    @Transactional(readOnly = true)
    public List<PaiementLeasing> findByMois(String mois) {
        return paiementLeasingRepository.findByMois(mois);
    }

    public PaiementLeasing marquerCommePaye(Long id) {
        PaiementLeasing paiement = findById(id);
        paiement.setStatutPaiement(StatutPaiement.PAYE);
        paiement.setDatePaiement(LocalDate.now());
        return paiementLeasingRepository.save(paiement);
    }

    public PaiementLeasing updateStatut(Long id, StatutPaiement statut) {
        PaiementLeasing paiement = findById(id);
        paiement.setStatutPaiement(statut);
        if (statut == StatutPaiement.PAYE && paiement.getDatePaiement() == null) {
            paiement.setDatePaiement(LocalDate.now());
        }
        return paiementLeasingRepository.save(paiement);
    }


    @Transactional(readOnly = true)
    public Double calculateTauxPaiement() {
        Long total = paiementLeasingRepository.count();
        if (total == 0) return 0.0;

        Long payes = paiementLeasingRepository.countByStatutPaiement(StatutPaiement.PAYE);
        return (payes.doubleValue() / total.doubleValue()) * 100;
    }


    @Transactional(readOnly = true)
    public List<PaiementLeasing> getPaiementsEnRetard() {
        return paiementLeasingRepository.findByStatutPaiement(StatutPaiement.EN_RETARD);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateMontantEnAttente() {
        BigDecimal montant = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.EN_ATTENTE);
        return montant != null ? montant : BigDecimal.ZERO;
    }

    
    @Transactional(readOnly = true)
    public BigDecimal calculateMontantTotalPaye() {
        BigDecimal montant = paiementLeasingRepository.calculateMontantTotalByStatut(StatutPaiement.PAYE);
        return montant != null ? montant : BigDecimal.ZERO;
    }
}
