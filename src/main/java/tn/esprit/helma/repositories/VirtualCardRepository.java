package tn.esprit.helma.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.helma.entities.VirtualCard;
import tn.esprit.helma.enums.CardStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité VirtualCard.
 * Fournit les opérations CRUD et des requêtes personnalisées pour les cartes virtuelles.
 */
@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, Long> {

    /**
     * Cherche une carte par son numéro
     */
    Optional<VirtualCard> findByCardNumber(String cardNumber);

    /**
     * Récupère toutes les cartes d'un compte bancaire
     */
    List<VirtualCard> findByBankAccountId(Long bankAccountId);

    /**
     * Récupère toutes les cartes actives d'un compte
     */
    @Query("SELECT vc FROM VirtualCard vc WHERE vc.bankAccount.id = :bankAccountId " +
           "AND vc.status = :status")
    List<VirtualCard> findActiveCardsByBankAccountId(@Param("bankAccountId") Long bankAccountId,
                                                     @Param("status") CardStatus status);

    /**
     * Compte les cartes d'un compte
     */
    long countByBankAccountId(Long bankAccountId);

    /**
     * Vérifie si un numéro de carte existe
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Récupère les cartes expirées
     */
    @Query("SELECT vc FROM VirtualCard vc WHERE vc.bankAccount.id = :bankAccountId " +
           "AND vc.expiryDate < :currentDate")
    List<VirtualCard> findExpiredCards(@Param("bankAccountId") Long bankAccountId,
                                       @Param("currentDate") String currentDate);

    /**
     * Récupère les cartes par statut donné
     */
    List<VirtualCard> findByBankAccountIdAndStatus(Long bankAccountId, CardStatus status);

    /**
     * Récupère les cartes dont la limite de paiement est supérieure à une valeur
     */
    List<VirtualCard> findByPaymentLimitGreaterThan(BigDecimal paymentLimit);

    /**
     * Récupère les cartes dont la limite de paiement est inférieure à une valeur
     */
    List<VirtualCard> findByPaymentLimitLessThan(BigDecimal paymentLimit);

    /**
     * Récupère les cartes avec dépenses mensuelles supérieures à une valeur
     */
    List<VirtualCard> findByBankAccountIdAndMonthlySpentGreaterThan(Long bankAccountId, BigDecimal monthlySpent);

    /**
     * Récupère les cartes avec dépenses mensuelles dans une plage donnée
     */
    List<VirtualCard> findByBankAccountIdAndMonthlySpentBetween(Long bankAccountId, BigDecimal minSpent, BigDecimal maxSpent);

    /**
     * Récupère les cartes d'un compte triées par limite de paiement décroissante
     */
    List<VirtualCard> findByBankAccountIdOrderByPaymentLimitDesc(Long bankAccountId);
}
