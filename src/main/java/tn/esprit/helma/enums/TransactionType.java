package tn.esprit.helma.enums;

/**
 * Énumération des types de transactions.
 * INTERNAL: Transfert entre comptes du même utilisateur
 * EXTERNAL: Transfert vers un compte tiers
 * CARD: Transaction par carte bancaire
 */
public enum TransactionType {
    INTERNAL("Transfert Interne"),
    EXTERNAL("Transfert Externe"),
    CARD("Paiement Carte");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
