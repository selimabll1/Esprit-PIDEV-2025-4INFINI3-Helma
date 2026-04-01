package tn.esprit.helma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.helma.entities.SavedBeneficiary;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité SavedBeneficiary.
 * Permet de rechercher rapidement des bénéficiaires favoris par utilisateur.
 */
@Repository
public interface SavedBeneficiaryRepository extends JpaRepository<SavedBeneficiary, Long> {

    /**
     * Vérifie l'existence d'un bénéficiaire par utilisateur et RIB.
     */
    boolean existsByUserIdAndBeneficiaryRib(Long userId, String beneficiaryRib);

    /**
     * Récupère tous les bénéficiaires d'un utilisateur.
     */
    List<SavedBeneficiary> findByUserId(Long userId);

    /**
     * Recherche un bénéficiaire par RIB.
     */
    Optional<SavedBeneficiary> findByUserIdAndBeneficiaryRib(Long userId, String beneficiaryRib);

    /**
     * Recherche un bénéficiaire par alias personnalisé.
     */
    Optional<SavedBeneficiary> findByUserIdAndAlias(Long userId, String alias);

    /**
     * Liste triée des bénéficiaires les plus utilisés.
     */
    @Query("SELECT sb FROM SavedBeneficiary sb WHERE sb.userId = :userId ORDER BY sb.transferCount DESC")
    List<SavedBeneficiary> findMostUsedBeneficiaries(@Param("userId") Long userId);

    /**
     * Compte le nombre total de bénéficiaires d'un utilisateur.
     */
    long countByUserId(Long userId);
}
