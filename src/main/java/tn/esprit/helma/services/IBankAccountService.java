package tn.esprit.helma.services;

import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour la gestion des comptes bancaires.
 * Définit les contrats métier pour les opérations sur les comptes.
 */
public interface IBankAccountService {

    /**
     * Crée un nouveau compte bancaire
     */
    BankAccount createAccount(String rib, AccountType accountType, String currency);

    /**
     * Récupère un compte par son ID
     */
    Optional<BankAccount> getAccountById(Long accountId);

    /**
     * Récupère un compte par son RIB
     */
    Optional<BankAccount> getAccountByRib(String rib);

    /**
     * Récupère tous les comptes d'un utilisateur
     */
    List<BankAccount> getUserAccounts(Long userId);

    /**
     * Récupère tous les comptes de l'utilisateur connecté
     */
    List<BankAccount> getCurrentUserAccounts();

    /**
     * Récupère tous les comptes actifs d'un utilisateur
     */
    List<BankAccount> getActiveUserAccounts(Long userId);

    /**
     * Récupère tous les comptes actifs de l'utilisateur connecté
     */
    List<BankAccount> getCurrentUserActiveAccounts();

    /**
     * Met à jour le solde du compte
     */
    BankAccount updateBalance(Long accountId, BigDecimal newBalance);

    /**
     * Crédite un compte (ajoute au solde)
     */
    BankAccount creditAccount(Long accountId, BigDecimal amount);

    /**
     * Débite un compte (soustrait du solde)
     */
    BankAccount debitAccount(Long accountId, BigDecimal amount);

    /**
     * Change le statut du compte
     */
    BankAccount updateAccountStatus(Long accountId, AccountStatus newStatus);

    /**
     * Gèle un compte temporairement
     */
    BankAccount freezeAccount(Long accountId);

    /**
     * Dégoèle un compte
     */
    BankAccount unfreezeAccount(Long accountId);

    /**
     * Ferme définitivement un compte
     */
    BankAccount closeAccount(Long accountId);

    /**
     * Supprime un compte
     */
    void deleteAccount(Long accountId);

    /**
     * Récupère le solde actuel d'un compte
     */
    BigDecimal getBalance(Long accountId);

    /**
     * Vérifie si un compte est actif
     */
    boolean isAccountActive(Long accountId);

    /**
     * Vérifie la disponibilité d'un RIB
     */
    boolean isRibAvailable(String rib);
}
