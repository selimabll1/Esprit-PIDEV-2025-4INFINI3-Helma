package com.helma.helmabackend.repository;

import com.helma.helmabackend.entity.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EquipementRepository extends JpaRepository<Equipement, Long> {
    List<Equipement> findByDisponible(Boolean disponible);
    List<Equipement> findByCategorie(String categorie);
    List<Equipement> findByPartenaireId(Long partenaireId);

    // Méthodes statistiques
    Long countByDisponible(Boolean disponible);

    @Query("SELECT SUM(e.valeur) FROM Equipement e")
    BigDecimal calculateValeurTotale();

    @Query("SELECT AVG(e.valeur) FROM Equipement e")
    BigDecimal calculateValeurMoyenne();

    @Query("SELECT e.categorie, COUNT(e) FROM Equipement e GROUP BY e.categorie")
    List<Object[]> countByCategorie();
}
