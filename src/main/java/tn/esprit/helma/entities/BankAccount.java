package tn.esprit.helma.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité BankAccount représentant un compte bancaire de l'utilisateur.
 * 
 * Responsabilité: Gérer les informations du compte bancaire, son solde et ses statuts.
 * Relations:
 * - OneToMany avec Transaction: Un compte peut avoir plusieurs transactions
 * - OneToMany avec VirtualCard: Un compte peut avoir plusieurs cartes virtuelles
 */
@Entity
@Table(name = "bank_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "transactions", "virtualCards"})
@ToString(exclude = {"user", "transactions", "virtualCards"})
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Utilisateur propriétaire du compte
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Identifiant brut du propriétaire. Permet de lire le user_id sans charger l'entité User.
     */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    /**
     * RIB (Relevé d'Identité Bancaire) - Identifiant unique du compte
     */
    @Column(nullable = false, unique = true, length = 27)
    private String rib;

    /**
     * Solde actuel du compte
     */
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Devise du compte (TND, EUR, USD, etc.)
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TND";

    /**
     * Type de compte (COURANT, EPARGNE, BUSINESS)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.COURANT;

    /**
     * Statut du compte (ACTIVE, FROZEN, CLOSED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * Date de création du compte
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
     * Liste des transactions effectuées depuis ce compte
     */
    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    /**
     * Liste des cartes virtuelles associées à ce compte
     */
    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VirtualCard> virtualCards;

    /**
     * Hash du PIN pour la sécurisation des transactions sensibles (nullable = compte peut ne pas avoir de PIN)
     */
    @Column(nullable = true, length = 100)
    private String pinHash;

    /**
     * Nombre de tentatives de PIN échouées (pour rate limiting)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer pinAttemptCount = 0;

    /**
     * Timestamp de la dernière tentative de PIN (pour calcul du lockout)
     */
    @Column(nullable = true)
    private LocalDateTime lastPinAttempt;

    /**
     * Compteur de transactions avec riskScore < 30 depuis la dernière vérification PIN
     * Après 3 tx, la 4ème demande un PIN (puis réinitialise le compteur à 0)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer pinCounterUnderThreshold = 0;

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
