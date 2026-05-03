package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "portfolio_imported_position",
        indexes = {
                @Index(name = "idx_portfolio_imported_investor", columnList = "investor_user_id"),
                @Index(name = "idx_portfolio_imported_created", columnList = "created_at")
        }
)
public class PortfolioImportedPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "investorUserId is required")
    @Column(name = "investor_user_id", nullable = false, updatable = false)
    private Long investorUserId;

    @NotNull(message = "campaignBusinessName is required")
    @Size(max = 180, message = "Campaign name must be at most 180 characters")
    @Column(name = "campaign_business_name", nullable = false, length = 180)
    private String campaignBusinessName;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_sector", length = 80)
    private SubSector subSector;

    @Size(max = 120, message = "Governorate must be at most 120 characters")
    @Column(length = 120)
    private String governorate;

    @Size(max = 120, message = "City must be at most 120 characters")
    @Column(length = 120)
    private String city;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "portfolio_imported_position_tags",
            joinColumns = @JoinColumn(name = "portfolio_imported_position_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 80)
    private Set<AppTag> tags = new LinkedHashSet<>();

    @NotNull(message = "investedAmount is required")
    @DecimalMin(value = "1.000", inclusive = true, message = "Amount must be at least 1")
    @Digits(integer = 16, fraction = 3, message = "Amount must have up to 16 digits and 3 decimals")
    @Column(name = "invested_amount", nullable = false, precision = 19, scale = 3)
    private BigDecimal investedAmount = BigDecimal.ZERO;

    @NotNull(message = "currency is required")
    @Size(max = 10, message = "Currency must be at most 10 characters")
    @Column(nullable = false, length = 10)
    private String currency = "TND";

    @Column(name = "campaign_funding_goal", precision = 19, scale = 3)
    private BigDecimal campaignFundingGoal;

    @Column(name = "campaign_raised_amount", precision = 19, scale = 3)
    private BigDecimal campaignRaisedAmount;

    @Column(name = "equity_offered_percent", precision = 9, scale = 6)
    private BigDecimal equityOfferedPercent;

    @Column(name = "ownership_percent", precision = 9, scale = 6)
    private BigDecimal ownershipPercent;

    @Column(name = "invested_at")
    private Instant investedAt;

    @Size(max = 120, message = "Source reference must be at most 120 characters")
    @Column(name = "source_reference", length = 120)
    private String sourceReference;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalize();
    }

    private void normalize() {
        campaignBusinessName = clean(campaignBusinessName);
        governorate = clean(governorate);
        city = clean(city);
        sourceReference = clean(sourceReference);
        notes = clean(notes);

        if (currency == null || currency.isBlank()) {
            currency = "TND";
        } else {
            currency = currency.trim().toUpperCase();
        }

        if (investedAmount == null) {
            investedAmount = BigDecimal.ZERO;
        }

        if (tags == null) {
            tags = new LinkedHashSet<>();
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() { return id; }

    public Long getInvestorUserId() { return investorUserId; }
    public void setInvestorUserId(Long investorUserId) { this.investorUserId = investorUserId; }

    public String getCampaignBusinessName() { return campaignBusinessName; }
    public void setCampaignBusinessName(String campaignBusinessName) { this.campaignBusinessName = campaignBusinessName; }

    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }

    public SubSector getSubSector() { return subSector; }
    public void setSubSector(SubSector subSector) { this.subSector = subSector; }

    public String getGovernorate() { return governorate; }
    public void setGovernorate(String governorate) { this.governorate = governorate; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Set<AppTag> getTags() { return tags; }
    public void setTags(Set<AppTag> tags) { this.tags = tags; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getCampaignFundingGoal() { return campaignFundingGoal; }
    public void setCampaignFundingGoal(BigDecimal campaignFundingGoal) { this.campaignFundingGoal = campaignFundingGoal; }

    public BigDecimal getCampaignRaisedAmount() { return campaignRaisedAmount; }
    public void setCampaignRaisedAmount(BigDecimal campaignRaisedAmount) { this.campaignRaisedAmount = campaignRaisedAmount; }

    public BigDecimal getEquityOfferedPercent() { return equityOfferedPercent; }
    public void setEquityOfferedPercent(BigDecimal equityOfferedPercent) { this.equityOfferedPercent = equityOfferedPercent; }

    public BigDecimal getOwnershipPercent() { return ownershipPercent; }
    public void setOwnershipPercent(BigDecimal ownershipPercent) { this.ownershipPercent = ownershipPercent; }

    public Instant getInvestedAt() { return investedAt; }
    public void setInvestedAt(Instant investedAt) { this.investedAt = investedAt; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
