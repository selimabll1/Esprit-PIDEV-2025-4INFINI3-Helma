package tn.esprit.projet_pi.dto.request;

import jakarta.validation.constraints.*; // 🔥 annotations de validation
import tn.esprit.projet_pi.enums.LoanType;

import java.math.BigDecimal;

public class LoanRequestDTO {

    // 🔹 Identifiant utilisateur (simulation microservice Users)
    @NotNull(message = "User ID is required")
    private Long userId;

    // 🔹 Type de prêt (PERSONAL, HOME, CAR...)
    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    // 🔹 Montant du prêt
    @NotNull(message = "Principal amount is required")
    @Positive(message = "Principal amount must be positive")
    private BigDecimal principalAmount;

    // 🔹 Durée en mois
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 600, message = "Duration too large") // optionnel mais pro
    private Integer durationMonths;

    // 🔹 Constructeur vide (obligatoire pour Spring)
    public LoanRequestDTO() {
    }

    // 🔹 Getters & Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }
}