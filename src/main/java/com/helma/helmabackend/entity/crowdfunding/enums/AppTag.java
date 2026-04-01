package com.helma.helmabackend.entity.crowdfunding.enums;

public enum AppTag {
    B2B(8),
    B2C(5),
    MARKETPLACE(7),
    SUBSCRIPTION(8),
    HARDWARE(3),
    SOFTWARE(7),
    MOBILE_APP(5),
    D2C(6),
    RECURRING_REVENUE(9),
    DEEPTECH(6),
    IMPACT(6),
    SUSTAINABLE(7),
    LOCAL(3),
    GLOBAL(7),
    SMB(5),
    ENTERPRISE(7),
    CONSUMER_BRAND(6),
    REGULATED(4),
    DATA_DRIVEN(7),
    AUTOMATION(7);

    private final int weight;

    AppTag(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}