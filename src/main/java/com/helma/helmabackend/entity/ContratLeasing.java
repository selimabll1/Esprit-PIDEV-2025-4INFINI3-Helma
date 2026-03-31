package com.helma.helmabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "contrat_leasing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false, unique = true)
    private DemandeLeasing demande;

    @Column(name = "loyer_mensuel", nullable = false, precision = 15, scale = 2)
    private BigDecimal loyerMensuel;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    //@Builder.Default
    private StatutContrat statut = StatutContrat.ACTIF;

    @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaiementLeasing> paiements;
}

