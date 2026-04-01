package tn.esprit.helma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.enums.AccountStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité BankAccount.
 * Fournit les opérations CRUD et des requêtes personnalisées pour les comptes bancaires.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * Cherche un compte par son RIB unique
     */
    Optional<BankAccount> findByRib(String rib);

    /**
     * Cherche tous les comptes d'un utilisateur
     */
    List<BankAccount> findByUserId(Long userId);

    /**
     * Cherche tous les comptes actifs d'un utilisateur
     */
    @Query("SELECT ba FROM BankAccount ba WHERE ba.userId = :userId AND ba.status = :status")
    List<BankAccount> findActiveAccountsByUserId(@Param("userId") Long userId, @Param("status") AccountStatus status);

    /**
     * Vérifie si un RIB existe dans la base de données
     */
    boolean existsByRib(String rib);

    /**
     * Compte tous les comptes d'un utilisateur
     */
    long countByUserId(Long userId);

    /**
     * Récupère les comptes par type
     */
    List<BankAccount> findByAccountType(String accountType);

    /**
     * Récupère les comptes dont le solde est supérieur à une valeur donnée
     */
    List<BankAccount> findByBalanceGreaterThan(BigDecimal balance);

    /**
     * Récupère les comptes dont le solde est inférieur à une valeur donnée
     */
    List<BankAccount> findByBalanceLessThan(BigDecimal balance);

    /**
     * Récupère les comptes dont le solde est entre deux valeurs
     */
    List<BankAccount> findByBalanceBetween(BigDecimal minBalance, BigDecimal maxBalance);

    /**
     * Récupère les comptes d'un utilisateur et les trie par solde décroissant
     */
    List<BankAccount> findByUserIdOrderByBalanceDesc(Long userId);

    /**
     * Récupère les comptes actifs avec un solde supérieur à une valeur
     */
    List<BankAccount> findByUserIdAndStatusAndBalanceGreaterThan(Long userId, AccountStatus status, BigDecimal balance);

    /**
     * Récupère les comptes par devise/monnaie
     */
    List<BankAccount> findByCurrency(String currency);
}
