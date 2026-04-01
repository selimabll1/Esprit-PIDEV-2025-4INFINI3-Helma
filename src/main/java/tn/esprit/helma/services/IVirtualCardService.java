package tn.esprit.helma.services;

import tn.esprit.helma.entities.VirtualCard;
import tn.esprit.helma.enums.CardStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des cartes virtuelles.
 * Définit les contrats métier pour les opérations sur les cartes.
 */
public interface IVirtualCardService {

    /**
     * Crée une nouvelle carte virtuelle
     */
    VirtualCard createCard(Long bankAccountId, String expiryDate, String cvvHash, BigDecimal paymentLimit);

    /**
     * Récupère une carte par son ID
     */
    Optional<VirtualCard> getCardById(Long cardId);

    /**
     * Récupère une carte par son numéro
     */
    Optional<VirtualCard> getCardByNumber(String cardNumber);

    /**
     * Récupère toutes les cartes d'un compte bancaire
     */
    List<VirtualCard> getCardsByBankAccount(Long bankAccountId);

    /**
     * Récupère toutes les cartes actives d'un compte
     */
    List<VirtualCard> getActiveCardsByBankAccount(Long bankAccountId);

    /**
     * Bloque une carte
     */
    VirtualCard blockCard(Long cardId);

    /**
     * Débloque une carte
     */
    VirtualCard unblockCard(Long cardId);

    /**
     * Change le statut d'une carte
     */
    VirtualCard updateCardStatus(Long cardId, CardStatus newStatus);

    /**
     * Met à jour la limite de paiement mensuel
     */
    VirtualCard updatePaymentLimit(Long cardId, BigDecimal newLimit);

    /**
     * Réinitialise le compteur de dépenses mensuelles
     */
    VirtualCard resetMonthlySpent(Long cardId);

    /**
     * Ajoute aux dépenses mensuelles (enregistrement d'une transaction)
     */
    VirtualCard addMonthlySpending(Long cardId, BigDecimal amount);

    /**
     * Vérifie si la limite mensuelle est atteinte
     */
    boolean isMonthlyLimitReached(Long cardId);

    /**
     * Supprime une carte
     */
    void deleteCard(Long cardId);

    /**
     * Vérifie si un numéro de carte existe
     */
    boolean cardNumberExists(String cardNumber);

    /**
     * Compte les cartes d'un compte
     */
    long countCardsByBankAccount(Long bankAccountId);
}
