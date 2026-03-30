package tn.esprit.projet_pi.dto.request;

import jakarta.validation.constraints.*; // 🔥 validation
import java.math.BigDecimal;

public class LoanSimulationDTO {

    // 🔹 Montant du prêt
    @NotNull(message = "Principal amount is required")
    @Positive(message = "Principal amount must be positive")
    private BigDecimal principalAmount;

    // 🔹 Taux d’intérêt (%)
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Interest rate must be positive")
    @DecimalMax(value = "100.0", message = "Interest rate too high")
    private BigDecimal interestRate;

    // 🔹 Durée en mois
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 600, message = "Duration too large")
    private Integer durationMonths;

    // 🔹 Constructeur vide
    public LoanSimulationDTO() {
    }

    // 🔹 Getters & Setters

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }
}