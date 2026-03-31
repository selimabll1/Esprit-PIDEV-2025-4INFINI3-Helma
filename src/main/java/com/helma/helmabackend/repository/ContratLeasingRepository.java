package com.helma.helmabackend.repository;

import com.helma.helmabackend.entity.ContratLeasing;
import com.helma.helmabackend.entity.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContratLeasingRepository extends JpaRepository<ContratLeasing, Long> {
    Optional<ContratLeasing> findByDemandeId(Long demandeId);
    List<ContratLeasing> findByStatut(StatutContrat statut);

    // Méthodes statistiques
    Long countByStatut(StatutContrat statut);

    @Query("SELECT SUM(c.loyerMensuel) FROM ContratLeasing c WHERE c.statut = 'ACTIF'")
    BigDecimal calculateRevenuMensuelTotal();

    @Query("SELECT AVG(c.loyerMensuel) FROM ContratLeasing c WHERE c.statut = 'ACTIF'")
    BigDecimal calculateLoyerMoyenMensuel();
}
