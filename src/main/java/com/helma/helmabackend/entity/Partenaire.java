package com.helma.helmabackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "partenaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partenaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nom;

    @NotBlank
    @Column(nullable = false)
    private String type;  // ex: FOURNISSEUR_IT, REVENDEUR, CONSTRUCTEUR

    @Column
    private String contact;

    @Column(nullable = false)
    //@Builder.Default
    private Boolean actif = true;
}

