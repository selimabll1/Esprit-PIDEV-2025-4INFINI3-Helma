package com.helma.helmabackend.entity.crowdfunding;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "equity_detail",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_equity_company_reg_number", columnNames = "company_registration_number")
        }
)
public class EquityDetail {

    @Id
    @Column(name = "application_raise_id")
    private Long applicationRaiseId;

    /**
     * PK=FK mapping:
     * EquityDetail.applicationRaiseId references ApplicationRaise.id
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "application_raise_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_equity_detail_application_raise")
    )
    private ApplicationRaise applicationRaise;

    @NotBlank(message = "companyLegalName is required")
    @Size(min = 2, max = 255, message = "companyLegalName must be between 2 and 255 characters")
    @Column(name = "company_legal_name", nullable = false, length = 255)
    private String companyLegalName;

    @NotBlank(message = "companyRegistrationNumber is required")
    @Size(min = 2, max = 120, message = "companyRegistrationNumber must be between 2 and 120 characters")
    @Column(name = "company_registration_number", nullable = false, length = 120)
    private String companyRegistrationNumber;

    @NotBlank(message = "cnreProfileUrl is required")
    @Size(max = 800, message = "cnreProfileUrl must be at most 800 characters")
    @Pattern(
            regexp = "^(https?://).+",
            message = "cnreProfileUrl must be a valid URL starting with http:// or https://"
    )
    @Column(name = "cnre_profile_url", nullable = false, length = 800)
    private String cnreProfileUrl;

    /**
     * Optional: 0 < percent <= 100
     */
    @DecimalMin(value = "0.01", inclusive = true, message = "equityOfferedPercent must be > 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "equityOfferedPercent must be <= 100")
    @Digits(integer = 3, fraction = 2, message = "equityOfferedPercent must have up to 3 digits and 2 decimals")
    @Column(name = "equity_offered_percent", precision = 5, scale = 2)
    private BigDecimal equityOfferedPercent;

    /**
     * Optional: >= 0
     */
    @DecimalMin(value = "0.000", inclusive = true, message = "preMoneyValuation cannot be negative")
    @Digits(integer = 11, fraction = 3, message = "preMoneyValuation must have up to 11 digits and 3 decimals")
    @Column(name = "pre_money_valuation", precision = 14, scale = 3)
    private BigDecimal preMoneyValuation;

    /**
     * Optional: >= 0
     */
    @DecimalMin(value = "0.000", inclusive = true, message = "minInvestment cannot be negative")
    @Digits(integer = 9, fraction = 3, message = "minInvestment must have up to 9 digits and 3 decimals")
    @Column(name = "min_investment", precision = 12, scale = 3)
    private BigDecimal minInvestment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        // basic normalization
        if (companyLegalName != null) companyLegalName = companyLegalName.trim();
        if (companyRegistrationNumber != null) companyRegistrationNumber = companyRegistrationNumber.trim();
        if (cnreProfileUrl != null) cnreProfileUrl = cnreProfileUrl.trim();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();

        if (companyLegalName != null) companyLegalName = companyLegalName.trim();
        if (companyRegistrationNumber != null) companyRegistrationNumber = companyRegistrationNumber.trim();
        if (cnreProfileUrl != null) cnreProfileUrl = cnreProfileUrl.trim();
    }

    // -------- getters/setters --------

    public Long getApplicationRaiseId() { return applicationRaiseId; }

    public ApplicationRaise getApplicationRaise() { return applicationRaise; }
    public void setApplicationRaise(ApplicationRaise applicationRaise) { this.applicationRaise = applicationRaise; }

    public String getCompanyLegalName() { return companyLegalName; }
    public void setCompanyLegalName(String companyLegalName) { this.companyLegalName = companyLegalName; }

    public String getCompanyRegistrationNumber() { return companyRegistrationNumber; }
    public void setCompanyRegistrationNumber(String companyRegistrationNumber) { this.companyRegistrationNumber = companyRegistrationNumber; }

    public String getCnreProfileUrl() { return cnreProfileUrl; }
    public void setCnreProfileUrl(String cnreProfileUrl) { this.cnreProfileUrl = cnreProfileUrl; }

    public BigDecimal getEquityOfferedPercent() { return equityOfferedPercent; }
    public void setEquityOfferedPercent(BigDecimal equityOfferedPercent) { this.equityOfferedPercent = equityOfferedPercent; }

    public BigDecimal getPreMoneyValuation() { return preMoneyValuation; }
    public void setPreMoneyValuation(BigDecimal preMoneyValuation) { this.preMoneyValuation = preMoneyValuation; }

    public BigDecimal getMinInvestment() { return minInvestment; }
    public void setMinInvestment(BigDecimal minInvestment) { this.minInvestment = minInvestment; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}