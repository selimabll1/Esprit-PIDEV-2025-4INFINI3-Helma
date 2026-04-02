package com.helma.helmabackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "demande_leasing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipement_id", nullable = false)
    private Equipement equipement;

    @Min(6)
    @Max(48)
    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois;

    @Column(name = "date_demande", nullable = false)
    //@Builder.Default
    private LocalDate dateDemande = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    //@Builder.Default
    private StatutDemande statut = StatutDemande.EN_ATTENTE;


    @Min(18)
    @Max(24)
    @Column(name = "age_demandeur", nullable = false)
    private Integer ageDemandeur;

    @OneToOne(mappedBy = "demande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private ContratLeasing contrat;
}

