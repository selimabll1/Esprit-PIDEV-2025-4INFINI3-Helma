package tn.esprit.helma.enums;

/**
 * Énumération des types de comptes bancaires.
 * COURANT: Compte chèques classique
 * EPARGNE: Compte d'épargne
 * BUSINESS: Compte professionnel
 */
public enum AccountType {
    COURANT("Compte Chèque"),
    EPARGNE("Compte Épargne"),
    BUSINESS("Compte Business");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
