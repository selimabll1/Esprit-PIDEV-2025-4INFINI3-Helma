package com.helma.helmabackend.entity.crowdfunding.enums;

public enum DocumentType {
    // Common
    ID_CARD,

    // Donation (optional but recommended)
    PROJECT_PITCH_DECK,

    // Equity required
    CNRE_EXTRACT,
    SHAREHOLDERS_CAP_TABLE,
    FINANCIAL_STATEMENTS,
    BANK_RIB

    // keep it minimal for now; add TAX_ID later if needed
}