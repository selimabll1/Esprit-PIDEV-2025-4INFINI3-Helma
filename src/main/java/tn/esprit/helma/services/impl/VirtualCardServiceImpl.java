package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.helma.entities.VirtualCard;
import tn.esprit.helma.enums.CardStatus;
import tn.esprit.helma.repositories.VirtualCardRepository;
import tn.esprit.helma.services.IVirtualCardService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des cartes virtuelles.
 * 
 * Responsabilités:
 * - Création et gestion des cartes virtuelles
 * - Gestion du statut (bloqué/débloqué)
 * - Suivi des dépenses mensuelles et limites
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class VirtualCardServiceImpl implements IVirtualCardService {

    private final VirtualCardRepository virtualCardRepository;

    /**
     * Crée une nouvelle carte virtuelle
     */
    @Override
    public VirtualCard createCard(Long bankAccountId, String expiryDate, String cvvHash, BigDecimal paymentLimit) {
        log.info("Création d'une carte virtuelle pour le compte: {}", bankAccountId);

        VirtualCard card = VirtualCard.builder()
                .bankAccount(null) // Sera défini par la relation
                .cardNumber(generateCardNumber())
                .expiryDate(expiryDate)
                .cvvHash(cvvHash)
                .status(CardStatus.ACTIVE)
                .paymentLimit(paymentLimit != null ? paymentLimit : BigDecimal.valueOf(5000.00))
                .monthlySpent(BigDecimal.ZERO)
                .build();

        VirtualCard savedCard = virtualCardRepository.save(card);
        log.info("Carte créée avec succès: ID={}, numéro={}...{}", 
                savedCard.getId(), savedCard.getCardNumber().substring(0, 4), 
                savedCard.getCardNumber().substring(12));

        return savedCard;
    }

    /**
     * Génère un numéro de carte unique (16 chiffres)
     */
    private String generateCardNumber() {
        // Format: 4532 XXXX XXXX XXXX (Visa)
        StringBuilder cardNumber = new StringBuilder("4532");
        for (int i = 0; i < 12; i++) {
            cardNumber.append((int)(Math.random() * 10));
        }
        return cardNumber.toString();
    }

    @Override
    public Optional<VirtualCard> getCardById(Long cardId) {
        log.debug("Récupération de la carte: {}", cardId);
        return virtualCardRepository.findById(cardId);
    }

    @Override
    public Optional<VirtualCard> getCardByNumber(String cardNumber) {
        log.debug("Recherche de la carte par numéro");
        return virtualCardRepository.findByCardNumber(cardNumber);
    }

    @Override
    public List<VirtualCard> getCardsByBankAccount(Long bankAccountId) {
        log.debug("Récupération des cartes du compte: {}", bankAccountId);
        return virtualCardRepository.findByBankAccountId(bankAccountId);
    }

    @Override
    public List<VirtualCard> getActiveCardsByBankAccount(Long bankAccountId) {
        log.debug("Récupération des cartes actives du compte: {}", bankAccountId);
        return virtualCardRepository.findActiveCardsByBankAccountId(bankAccountId, CardStatus.ACTIVE);
    }

    /**
     * Bloque une carte
     */
    @Override
    public VirtualCard blockCard(Long cardId) {
        log.warn("Blocage de la carte: {}", cardId);
        return updateCardStatus(cardId, CardStatus.BLOCKED);
    }

    /**
     * Débloque une carte
     */
    @Override
    public VirtualCard unblockCard(Long cardId) {
        log.info("Déblocage de la carte: {}", cardId);
        return updateCardStatus(cardId, CardStatus.ACTIVE);
    }

    /**
     * Change le statut d'une carte
     */
    @Override
    public VirtualCard updateCardStatus(Long cardId, CardStatus newStatus) {
        log.info("Changement du statut de la carte: {} -> {}", cardId, newStatus);

        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carte non trouvée"));

        card.setStatus(newStatus);
        card.setUpdatedAt(LocalDateTime.now());

        return virtualCardRepository.save(card);
    }

    /**
     * Met à jour la limite de paiement mensuel
     */
    @Override
    public VirtualCard updatePaymentLimit(Long cardId, BigDecimal newLimit) {
        log.info("Mise à jour de la limite de paiement de la carte: {} -> {}", cardId, newLimit);

        if (newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La limite doit être positive");
        }

        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carte non trouvée"));

        card.setPaymentLimit(newLimit);
        card.setUpdatedAt(LocalDateTime.now());

        return virtualCardRepository.save(card);
    }

    /**
     * Réinitialise le compteur de dépenses mensuelles (en début de mois)
     */
    @Override
    public VirtualCard resetMonthlySpent(Long cardId) {
        log.info("Réinitialisation des dépenses mensuelles de la carte: {}", cardId);

        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carte non trouvée"));

        card.setMonthlySpent(BigDecimal.ZERO);
        card.setUpdatedAt(LocalDateTime.now());

        return virtualCardRepository.save(card);
    }

    /**
     * Ajoute aux dépenses mensuelles (appelé lors d'une transaction)
     */
    @Override
    public VirtualCard addMonthlySpending(Long cardId, BigDecimal amount) {
        log.debug("Ajout aux dépenses mensuelles de la carte: {}, montant: {}", cardId, amount);

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carte non trouvée"));

        card.setMonthlySpent(card.getMonthlySpent().add(amount));
        card.setUpdatedAt(LocalDateTime.now());

        return virtualCardRepository.save(card);
    }

    /**
     * Vérifie si la limite mensuelle est atteinte
     */
    @Override
    public boolean isMonthlyLimitReached(Long cardId) {
        VirtualCard card = virtualCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Carte non trouvée"));

        return card.getMonthlySpent().compareTo(card.getPaymentLimit()) >= 0;
    }

    @Override
    public void deleteCard(Long cardId) {
        log.warn("Suppression de la carte: {}", cardId);
        virtualCardRepository.deleteById(cardId);
    }

    @Override
    public boolean cardNumberExists(String cardNumber) {
        return virtualCardRepository.existsByCardNumber(cardNumber);
    }

    @Override
    public long countCardsByBankAccount(Long bankAccountId) {
        return virtualCardRepository.countByBankAccountId(bankAccountId);
    }
}
