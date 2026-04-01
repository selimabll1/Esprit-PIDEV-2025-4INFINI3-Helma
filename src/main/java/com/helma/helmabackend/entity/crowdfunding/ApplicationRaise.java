package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "application_raise",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_application_raise_founder_business_name",
                        columnNames = {"founder_user_id", "business_name"}
                )
        },
        indexes = {
                @Index(name = "idx_application_raise_founder", columnList = "founder_user_id"),
                @Index(name = "idx_application_raise_status", columnList = "status"),
                @Index(name = "idx_application_raise_type", columnList = "type"),
                @Index(name = "idx_application_raise_sector", columnList = "sector"),
                @Index(name = "idx_application_raise_sub_sector", columnList = "sub_sector")
        }
)
public class ApplicationRaise {

    public static final BigDecimal MIN_FUNDING_GOAL_TND = new BigDecimal("500.000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "founderUserId is required")
    @Column(name = "founder_user_id", nullable = false, updatable = false)
    private Long founderUserId;

    @NotNull(message = "Crowdfunding type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CrowdfundingType type;

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 150, message = "Business name must be between 2 and 150 characters")
    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Size(max = 80, message = "Company number must be at most 80 characters")
    @Column(name = "company_number", length = 80, unique = true)
    private String companyNumber;

    @Size(max = 255, message = "Website must be at most 255 characters")
    @Pattern(
            regexp = "^(https?://)?([\\w-]+\\.)+[\\w-]{2,}(/.*)?$",
            message = "Website must be a valid URL (example: https://example.com)"
    )
    @Column(name = "website", length = 255)
    private String website;

    @NotBlank(message = "Country is required")
    @Size(max = 60, message = "Country must be at most 60 characters")
    @Column(name = "country", nullable = false, length = 60)
    private String country = "Tunisia";

    @NotBlank(message = "Currency is required")
    @Size(max = 10, message = "Currency must be at most 10 characters")
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "TND";

    @NotNull(message = "Sector is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "sector", nullable = false, length = 80)
    private Sector sector;

    @NotNull(message = "SubSector is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_sector", nullable = false, length = 80)
    private SubSector subSector;

    @NotEmpty(message = "At least one tag is required")
    @ElementCollection(targetClass = AppTag.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "application_raise_tag",
            joinColumns = @JoinColumn(name = "application_raise_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uq_application_raise_tag",
                            columnNames = {"application_raise_id", "tag"}
                    )
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 40)
    private Set<AppTag> tags = new LinkedHashSet<>();

    @NotBlank(message = "Summary is required")
    @Size(min = 10, max = 255, message = "Summary must be between 10 and 255 characters")
    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @NotNull(message = "Funding goal is required")
    @DecimalMin(value = "500.000", inclusive = true, message = "Funding goal must be at least 500 TND")
    @Digits(integer = 16, fraction = 3, message = "Funding goal must have up to 16 digits and 3 decimals")
    @Column(name = "funding_goal", nullable = false, precision = 19, scale = 3)
    private BigDecimal fundingGoal;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationRaiseStatus status = ApplicationRaiseStatus.DRAFT;

    @DecimalMin(value = "0.000", inclusive = true, message = "Investors pledged amount cannot be negative")
    @Digits(integer = 16, fraction = 3, message = "Investors pledged amount must have up to 16 digits and 3 decimals")
    @Column(name = "investors_pledged_amount", precision = 19, scale = 3)
    private BigDecimal investorsPledgedAmount = BigDecimal.ZERO;

    @Min(value = 0, message = "Customer count cannot be negative")
    @Max(value = 100000000, message = "Customer count is too large")
    @Column(name = "customer_count")
    private Integer customerCount;

    @NotBlank(message = "Contact first name is required")
    @Size(min = 2, max = 80, message = "Contact first name must be between 2 and 80 characters")
    @Column(name = "contact_first_name", nullable = false, length = 80)
    private String contactFirstName;

    @NotBlank(message = "Contact last name is required")
    @Size(min = 2, max = 80, message = "Contact last name must be between 2 and 80 characters")
    @Column(name = "contact_last_name", nullable = false, length = 80)
    private String contactLastName;

    @Size(max = 80, message = "Contact title must be at most 80 characters")
    @Column(name = "contact_title", length = 80)
    private String contactTitle;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be a valid email")
    @Size(max = 150, message = "Contact email must be at most 150 characters")
    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Pattern(
            regexp = "^(\\+216|216)?\\d{8}$",
            message = "Phone must be a Tunisian number (8 digits, optionally prefixed by +216 or 216)"
    )
    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @AssertTrue(message = "You must accept terms and conditions")
    @Column(name = "accepted_terms", nullable = false)
    private boolean acceptedTerms;

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
        businessName = normalizeRequired(businessName);
        summary = normalizeRequired(summary);

        contactFirstName = normalizeRequired(contactFirstName);
        contactLastName = normalizeRequired(contactLastName);
        contactEmail = normalizeRequired(contactEmail);

        companyNumber = normalizeOptional(companyNumber);
        website = normalizeOptional(website);
        contactTitle = normalizeOptional(contactTitle);
        contactPhone = normalizeOptional(contactPhone);

        if (country == null || country.isBlank()) country = "Tunisia";
        if (currency == null || currency.isBlank()) currency = "TND";

        if (investorsPledgedAmount == null) investorsPledgedAmount = BigDecimal.ZERO;

        if (tags == null) {
            tags = new LinkedHashSet<>();
        } else {
            tags.removeIf(Objects::isNull);
        }
    }

    private String normalizeRequired(String v) {
        if (v == null) return null;
        return v.trim();
    }

    private String normalizeOptional(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public Long getId() {
        return id;
    }

    public Long getFounderUserId() {
        return founderUserId;
    }

    public void setFounderUserId(Long founderUserId) {
        this.founderUserId = founderUserId;
    }

    public CrowdfundingType getType() {
        return type;
    }

    public void setType(CrowdfundingType type) {
        this.type = type;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public SubSector getSubSector() {
        return subSector;
    }

    public void setSubSector(SubSector subSector) {
        this.subSector = subSector;
    }

    public Set<AppTag> getTags() {
        return tags;
    }

    public void setTags(Set<AppTag> tags) {
        this.tags = tags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getFundingGoal() {
        return fundingGoal;
    }

    public void setFundingGoal(BigDecimal fundingGoal) {
        this.fundingGoal = fundingGoal;
    }

    public ApplicationRaiseStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationRaiseStatus status) {
        this.status = status;
    }

    public BigDecimal getInvestorsPledgedAmount() {
        return investorsPledgedAmount;
    }

    public void setInvestorsPledgedAmount(BigDecimal investorsPledgedAmount) {
        this.investorsPledgedAmount = investorsPledgedAmount;
    }

    public Integer getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(Integer customerCount) {
        this.customerCount = customerCount;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getContactTitle() {
        return contactTitle;
    }

    public void setContactTitle(String contactTitle) {
        this.contactTitle = contactTitle;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}