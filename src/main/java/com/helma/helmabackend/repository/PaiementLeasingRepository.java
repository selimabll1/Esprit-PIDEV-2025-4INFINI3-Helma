package com.helma.helmabackend.repository;

import com.helma.helmabackend.entity.PaiementLeasing;
import com.helma.helmabackend.entity.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaiementLeasingRepository extends JpaRepository<PaiementLeasing, Long> {
    List<PaiementLeasing> findByContratId(Long contratId);
    List<PaiementLeasing> findByStatutPaiement(StatutPaiement statutPaiement);
    List<PaiementLeasing> findByMois(String mois);

    // Méthodes statistiques
    Long countByStatutPaiement(StatutPaiement statutPaiement);

    @Query("SELECT SUM(p.montant) FROM PaiementLeasing p WHERE p.statutPaiement = :statut")
    BigDecimal calculateMontantTotalByStatut(StatutPaiement statut);

    @Query("SELECT p.mois, SUM(p.montant), COUNT(p) " +
           "FROM PaiementLeasing p " +
           "GROUP BY p.mois " +
           "ORDER BY p.mois DESC")
    List<Object[]> findRevenusParMoisRaw();

    @Query("SELECT p.mois, SUM(CASE WHEN p.statutPaiement = 'PAYE' THEN p.montant ELSE 0 END), " +
           "SUM(p.montant), COUNT(p) " +
           "FROM PaiementLeasing p " +
           "GROUP BY p.mois " +
           "ORDER BY p.mois DESC")
    List<Object[]> findRevenusDetaillesParMoisRaw();
}
