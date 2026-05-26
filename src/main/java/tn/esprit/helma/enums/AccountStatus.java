package tn.esprit.helma.enums;

/**
 * Énumération des statuts de compte bancaire.
 * ACTIVE: Compte actif et utilisable
 * FROZEN: Compte gelé temporairement
 * CLOSED: Compte fermé définitivement
 */
public enum AccountStatus {
    ACTIVE("Actif"),
    FROZEN("Gelé"),
    CLOSED("Fermé");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
