package tn.esprit.helma.enums;

/**
 * Énumération des statuts de transaction.
 * PENDING: Transaction en attente de confirmation
 * CONFIRMED: Transaction confirmée et exécutée
 * SUSPICIOUS: Transaction détectée comme suspecte
 * CANCELED: Transaction annulée
 */
public enum TransactionStatus {
    PENDING("En Attente"),
    CONFIRMED("Confirmé"),
    SUSPICIOUS("Suspect"),
    CANCELED("Annulé");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
