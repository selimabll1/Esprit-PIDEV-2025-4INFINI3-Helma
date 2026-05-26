package tn.esprit.helma.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité SavedBeneficiary représentant un bénéficiaire enregistré.
 * 
 * Responsabilité: Gérer la liste des bénéficiaires enregistrés pour les transferts rapides.
 * Une relation directe avec Transaction n'existe pas, mais les bénéficiaires peuvent être
 * réutilisés pour plusieurs transactions.
 */
@Entity
@Table(name = "saved_beneficiary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant de l'utilisateur propriétaire
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Nom du bénéficiaire
     */
    @Column(nullable = false, length = 100)
    private String beneficiaryName;

    /**
     * RIB du bénéficiaire (27 caractères pour un RIB tunisien)
     */
    @Column(nullable = false, length = 27)
    private String beneficiaryRib;

    /**
     * Alias personnalisé pour le bénéficiaire (ex: "Mère", "Entreprise XYZ")
     */
    @Column(length = 50)
    private String alias;

    /**
     * Nombre de transferts effectués vers ce bénéficiaire
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer transferCount = 0;

    /**
     * Date de création du bénéficiaire
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
