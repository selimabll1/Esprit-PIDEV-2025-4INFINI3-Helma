package com.helma.helmabackend.repository;

import com.helma.helmabackend.entity.DemandeLeasing;
import com.helma.helmabackend.entity.StatutDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandeLeasingRepository extends JpaRepository<DemandeLeasing, Long> {
    List<DemandeLeasing> findByUserId(Long userId);
    List<DemandeLeasing> findByStatut(StatutDemande statut);
    List<DemandeLeasing> findByEquipementId(Long equipementId);

    // Méthodes statistiques
    Long countByStatut(StatutDemande statut);

    @Query("SELECT AVG(d.ageDemandeur) FROM DemandeLeasing d")
    Double findAgeMoyenDemandeurs();

    @Query("SELECT AVG(d.dureeMois) FROM DemandeLeasing d")
    Double findDureeMoyenneMois();

    @Query("SELECT d.equipement.id, d.equipement.nom, d.equipement.categorie, COUNT(d) as total " +
           "FROM DemandeLeasing d " +
           "GROUP BY d.equipement.id, d.equipement.nom, d.equipement.categorie " +
           "ORDER BY total DESC")
    List<Object[]> findEquipementsLesPlusDemandesRaw();
}
