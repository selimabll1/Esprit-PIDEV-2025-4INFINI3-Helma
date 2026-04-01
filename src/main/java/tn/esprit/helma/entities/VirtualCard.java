package tn.esprit.helma.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.helma.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité VirtualCard représentant une carte virtuelle.
 * 
 * Responsabilité: Gérer les informations des cartes virtuelles attachées à un compte.
 * Relations:
 * - ManyToOne avec BankAccount: Une carte appartient à un seul compte
 */
@Entity
@Table(name = "virtual_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Compte bancaire propriétaire de la carte
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    /**
     * Numéro de carte virtuelle (16 chiffres)
     */
    @Column(nullable = false, unique = true, length = 16)
    private String cardNumber;

    /**
     * Date d'expiration (format: MM/YY)
     */
    @Column(nullable = false, length = 5)
    private String expiryDate;

    /**
     * Hash sécurisé du CVV (ne jamais stocker en clair)
     */
    @Column(nullable = false)
    private String cvvHash;

    /**
     * Statut de la carte (ACTIVE, BLOCKED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    /**
     * Limite de paiement mensuelle pour la carte
     */
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paymentLimit = BigDecimal.valueOf(5000.00);

    /**
     * Montant dépensé ce mois-ci
     */
    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    /**
     * Date de création de la carte
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de dernière modification
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mise à jour automatique de la date de modification avant chaque sauvegarde
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Initialisation de updatedAt avant la première insertion
     */
    @PrePersist
    public void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }
}
