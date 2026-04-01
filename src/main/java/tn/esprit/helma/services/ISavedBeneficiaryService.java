package tn.esprit.helma.services;

import tn.esprit.helma.entities.SavedBeneficiary;

import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des bénéficiaires enregistrés.
 * Facilite les transferts récurrents vers les mêmes bénéficiaires.
 */
public interface ISavedBeneficiaryService {

    /**
     * Enregistre un nouveau bénéficiaire
     */
    SavedBeneficiary saveBeneficiary(Long userId, String beneficiaryName, String beneficiaryRib, String alias);

    /**
     * Récupère un bénéficiaire par son ID
     */
    Optional<SavedBeneficiary> getBeneficiaryById(Long beneficiaryId);

    /**
     * Récupère tous les bénéficiaires d'un utilisateur
     */
    List<SavedBeneficiary> getUserBeneficiaries(Long userId);

    /**
     * Cherche un bénéficiaire par RIB
     */
    Optional<SavedBeneficiary> getBeneficiaryByRib(Long userId, String beneficiaryRib);

    /**
     * Cherche un bénéficiaire par alias
     */
    Optional<SavedBeneficiary> getBeneficiaryByAlias(Long userId, String alias);

    /**
     * Récupère les bénéficiaires les plus utilisés
     */
    List<SavedBeneficiary> getMostUsedBeneficiaries(Long userId);

    /**
     * Met à jour un bénéficiaire
     */
    SavedBeneficiary updateBeneficiary(Long beneficiaryId, String newName, String newAlias);

    /**
     * Incrémente le compteur de transferts
     */
    SavedBeneficiary incrementTransferCount(Long beneficiaryId);

    /**
     * Supprime un bénéficiaire
     */
    void deleteBeneficiary(Long beneficiaryId);

    /**
     * Vérifie si un bénéficiaire existe pour un utilisateur
     */
    boolean beneficiaryExists(Long userId, String beneficiaryRib);

    /**
     * Compte les bénéficiaires d'un utilisateur
     */
    long countUserBeneficiaries(Long userId);
}
