package tn.esprit.helma.enums;

/**
 * Énumération des statuts de carte virtuelle.
 * ACTIVE: Carte active et utilisable
 * BLOCKED: Carte bloquée
 */
public enum CardStatus {
    ACTIVE("Actif"),
    BLOCKED("Bloquée");

    private final String label;

    CardStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
