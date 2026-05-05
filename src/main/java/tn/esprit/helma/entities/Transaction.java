package tn.esprit.helma.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.helma.enums.TransactionPeriodicity;
import tn.esprit.helma.enums.TransactionStatus;
import tn.esprit.helma.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Transaction représentant une transaction bancaire.
 * 
 * Responsabilité: Enregistrer toutes les transactions (transferts, paiements, etc.) 
 * effectuées depuis un compte bancaire.
 * 
 * Relations:
 * - ManyToOne avec BankAccount: Une transaction appartient à un seul compte
 */
@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Compte bancaire à partir duquel la transaction est effectuée
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    /**
     * Nom du bénéficiaire (peut être un compte interne ou externe)
     */
    @Column(nullable = false, length = 100)
    private String beneficiaryName;

    /**
     * RIB du bénéficiaire (pour les transferts externes)
     */
    @Column(length = 27)
    private String beneficiaryRib;

    /**
     * Montant de la transaction
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Type de transaction (INTERNAL, EXTERNAL, CARD)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /**
     * Catégorie de la transaction (ex: "Salaire", "Facture", "Épicerie")
     */
    @Column(length = 50)
    private String category;

    /**
     * Description libre de la transaction
     */
    @Column(length = 500)
    private String description;

    /**
     * Périodicité de la transaction (NOW, SCHEDULED, PERMANENT)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionPeriodicity periodicity = TransactionPeriodicity.NOW;

    /**
     * Date programmée de la transaction (pour les transferts futurs)
     */
    @Column
    private LocalDateTime scheduledDate;

    /**
     * Date de prochaine exécution (pour les transactions permanentes)
     */
    @Column
    private LocalDateTime nextExecutionDate;

    /**
     * Date de dernière exécution (pour les transactions permanentes)
     */
    @Column
    private LocalDateTime lastExecutionDate;

    /**
     * Statut de la transaction (PENDING, CONFIRMED, SUSPICIOUS, CANCELED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Score de risque (0-100) indiquant la probabilité d'une transaction frauduleuse
     * Calculé en temps réel par le système de détection de fraude
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer riskScore = 0;

    /**
     * Date de création de la transaction
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de confirmation de la transaction
     */
    @Column
    private LocalDateTime confirmedAt;

    /**
     * Montant converti dans la devise du bénéficiaire (null si pas de conversion)
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    /**
     * Taux de change appliqué (null si pas de conversion)
     */
    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * Devise cible du bénéficiaire (null si même devise)
     */
    @Column(length = 3)
    private String targetCurrency;

    /**
     * Mise à jour automatique de confirmedAt si le statut change à CONFIRMED
     */
    @PreUpdate
    public void onUpdate() {
        if (this.status == TransactionStatus.CONFIRMED && this.confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }
}
