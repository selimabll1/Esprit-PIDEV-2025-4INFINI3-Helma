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
@EqualsAndHashCode(exclude = {"transactions", "virtualCards"})
@ToString(exclude = {"transactions", "virtualCards"})
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant de l'utilisateur propriétaire du compte
     */
    @Column(nullable = false)
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
