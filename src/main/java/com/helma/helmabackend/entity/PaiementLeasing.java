package com.helma.helmabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "paiement_leasing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_id", nullable = false)
    private ContratLeasing contrat;

    // Mois concerné : ex. 2025-03
    @Column(nullable = false)
    private String mois;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement ;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;
}
