package com.helma.helmabackend.dto.crowdfunding.rne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RneShortDetailsResponse {
    private String typeRegistre;
    private String categorieRegistre;
    private String idUnique;

    private String formeJuridiqueAr;
    private String formeJuridiqueFr;

    private String activiteExerceeAr;
    private String activiteExerceeFr;

    private String etatRegistreAr;
    private String etatRegistreFr;

    private String nomCommercialAr;
    private String nomCommercialFr;

    private String denominationLatin;
    private String denomination;

    private String rueAr;
    private String rueFr;

    private String codePostal;
    private String villeAr;
    private String villeFr;

    private String objetActivitePrincipaleAr;
    private String objetActivitePrincipaleFr;

    private String natureAssociation;
    private String situationFiscale;

    private String nomFr;
    private String prenomFr;
    private String nomAr;
    private String prenomAr;

    private String status;
}