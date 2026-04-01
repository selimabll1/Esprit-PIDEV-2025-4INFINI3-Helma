package tn.esprit.helma.enums;

/**
 * Énumération de la périodicité des transactions.
 * 
 * NOW: Transaction immédiate (exécutée maintenant)
 * SCHEDULED: Transaction programmée (exécutée à une date spécifique)
 * PERMANENT: Transaction permanente/récurrente (exécutée régulièrement)
 */
public enum TransactionPeriodicity {
    NOW("Maintenant"),
    SCHEDULED("Programmé"),
    PERMANENT("Permanent");

    private final String label;

    TransactionPeriodicity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
