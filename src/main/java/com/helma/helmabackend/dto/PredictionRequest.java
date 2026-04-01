package com.helma.helmabackend.dto;

public class PredictionRequest {
    private int age_demandeur;
    private int duree_mois;
    private double loyer_mensuel;
    private double montant;
    private double valeur;
    private int disponible;
    private int actif;
    private String categorie;
    private String type;
    private String statut_contrat;
    private String statut_demande;

    // Getters & Setters
    public int getAge_demandeur() { return age_demandeur; }
    public void setAge_demandeur(int age_demandeur) { this.age_demandeur = age_demandeur; }
    public int getDuree_mois() { return duree_mois; }
    public void setDuree_mois(int duree_mois) { this.duree_mois = duree_mois; }
    public double getLoyer_mensuel() { return loyer_mensuel; }
    public void setLoyer_mensuel(double loyer_mensuel) { this.loyer_mensuel = loyer_mensuel; }
    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }
    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = valeur; }
    public int getDisponible() { return disponible; }
    public void setDisponible(int disponible) { this.disponible = disponible; }
    public int getActif() { return actif; }
    public void setActif(int actif) { this.actif = actif; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatut_contrat() { return statut_contrat; }
    public void setStatut_contrat(String statut_contrat) { this.statut_contrat = statut_contrat; }
    public String getStatut_demande() { return statut_demande; }
    public void setStatut_demande(String statut_demande) { this.statut_demande = statut_demande; }

}
