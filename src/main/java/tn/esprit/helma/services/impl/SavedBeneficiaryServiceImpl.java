package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.helma.entities.SavedBeneficiary;
import tn.esprit.helma.repositories.SavedBeneficiaryRepository;
import tn.esprit.helma.services.ISavedBeneficiaryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des bénéficiaires enregistrés.
 * 
 * Responsabilités:
 * - Enregistrement et gestion des bénéficiaires fréquents
 * - Suivi des transferts vers chaque bénéficiaire
 * - Facilite les transferts rapides et récurrents
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class SavedBeneficiaryServiceImpl implements ISavedBeneficiaryService {

    private final SavedBeneficiaryRepository savedBeneficiaryRepository;

    /**
     * Enregistre un nouveau bénéficiaire
     */
    @Override
    public SavedBeneficiary saveBeneficiary(Long userId, String beneficiaryName, String beneficiaryRib, String alias) {
        log.info("Enregistrement d'un bénéficiaire pour l'utilisateur: {}", userId);

        // Vérifier que le bénéficiaire n'existe pas déjà
        if (savedBeneficiaryRepository.existsByUserIdAndBeneficiaryRib(userId, beneficiaryRib)) {
            log.warn("Le bénéficiaire {} existe déjà pour l'utilisateur: {}", beneficiaryRib, userId);
            throw new IllegalArgumentException("Ce bénéficiaire existe déjà");
        }

        SavedBeneficiary beneficiary = SavedBeneficiary.builder()
                .userId(userId)
                .beneficiaryName(beneficiaryName)
                .beneficiaryRib(beneficiaryRib)
                .alias(alias)
                .transferCount(0)
                .build();

        SavedBeneficiary savedBeneficiary = savedBeneficiaryRepository.save(beneficiary);
        log.info("Bénéficiaire enregistré avec succès: ID={}, alias={}", savedBeneficiary.getId(), alias);

        return savedBeneficiary;
    }

    @Override
    public Optional<SavedBeneficiary> getBeneficiaryById(Long beneficiaryId) {
        log.debug("Récupération du bénéficiaire: {}", beneficiaryId);
        return savedBeneficiaryRepository.findById(beneficiaryId);
    }

    @Override
    public List<SavedBeneficiary> getUserBeneficiaries(Long userId) {
        log.debug("Récupération des bénéficiaires de l'utilisateur: {}", userId);
        return savedBeneficiaryRepository.findByUserId(userId);
    }

    @Override
    public Optional<SavedBeneficiary> getBeneficiaryByRib(Long userId, String beneficiaryRib) {
        log.debug("Recherche du bénéficiaire par RIB pour l'utilisateur: {}", userId);
        return savedBeneficiaryRepository.findByUserIdAndBeneficiaryRib(userId, beneficiaryRib);
    }

    @Override
    public Optional<SavedBeneficiary> getBeneficiaryByAlias(Long userId, String alias) {
        log.debug("Recherche du bénéficiaire par alias pour l'utilisateur: {}", userId);
        return savedBeneficiaryRepository.findByUserIdAndAlias(userId, alias);
    }

    @Override
    public List<SavedBeneficiary> getMostUsedBeneficiaries(Long userId) {
        log.debug("Récupération des bénéficiaires les plus utilisés pour l'utilisateur: {}", userId);
        return savedBeneficiaryRepository.findMostUsedBeneficiaries(userId);
    }

    /**
     * Met à jour un bénéficiaire
     */
    @Override
    public SavedBeneficiary updateBeneficiary(Long beneficiaryId, String newName, String newAlias) {
        log.info("Mise à jour du bénéficiaire: {}", beneficiaryId);

        SavedBeneficiary beneficiary = savedBeneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Bénéficiaire non trouvé"));

        if (newName != null && !newName.isEmpty()) {
            beneficiary.setBeneficiaryName(newName);
        }

        if (newAlias != null && !newAlias.isEmpty()) {
            beneficiary.setAlias(newAlias);
        }

        beneficiary.setUpdatedAt(LocalDateTime.now());

        return savedBeneficiaryRepository.save(beneficiary);
    }

    /**
     * Incrémente le compteur de transferts
     */
    @Override
    public SavedBeneficiary incrementTransferCount(Long beneficiaryId) {
        log.debug("Incrémentation du compteur de transferts pour le bénéficiaire: {}", beneficiaryId);

        SavedBeneficiary beneficiary = savedBeneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new IllegalArgumentException("Bénéficiaire non trouvé"));

        beneficiary.setTransferCount(beneficiary.getTransferCount() + 1);
        beneficiary.setUpdatedAt(LocalDateTime.now());

        return savedBeneficiaryRepository.save(beneficiary);
    }

    @Override
    public void deleteBeneficiary(Long beneficiaryId) {
        log.warn("Suppression du bénéficiaire: {}", beneficiaryId);
        savedBeneficiaryRepository.deleteById(beneficiaryId);
    }

    @Override
    public boolean beneficiaryExists(Long userId, String beneficiaryRib) {
        return savedBeneficiaryRepository.existsByUserIdAndBeneficiaryRib(userId, beneficiaryRib);
    }

    @Override
    public long countUserBeneficiaries(Long userId) {
        return savedBeneficiaryRepository.countByUserId(userId);
    }
}
