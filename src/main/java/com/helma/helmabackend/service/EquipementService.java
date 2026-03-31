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

    public Equipement create(Equipement equipement) {
        return equipementRepository.save(equipement);
    }

    public Equipement update(Long id, Equipement equipement) {
        Equipement existing = findById(id);
        existing.setNom(equipement.getNom());
        existing.setCategorie(equipement.getCategorie());
        existing.setValeur(equipement.getValeur());
        existing.setDisponible(equipement.getDisponible());
        existing.setPartenaire(equipement.getPartenaire());
        return equipementRepository.save(existing);
    }

    public void delete(Long id) {
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

    /**
     * Calcule le taux d'utilisation des équipements
     */
    @Transactional(readOnly = true)
    public Double calculateTauxUtilisation() {
        Long total = equipementRepository.count();
        if (total == 0) return 0.0;

        Long enLocation = equipementRepository.countByDisponible(false);
        return (enLocation.doubleValue() / total.doubleValue()) * 100;
    }

    /**
     * Calcule la valeur totale du parc d'équipements
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateValeurTotale() {
        BigDecimal valeur = equipementRepository.calculateValeurTotale();
        return valeur != null ? valeur : BigDecimal.ZERO;
    }

    /**
     * Récupère uniquement les équipements disponibles
     */
    @Transactional(readOnly = true)
    public List<Equipement> getEquipementsDisponibles() {
        return equipementRepository.findByDisponible(true);
    }
}
