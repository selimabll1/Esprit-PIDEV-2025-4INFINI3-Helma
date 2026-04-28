package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseDraftStep;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.CurrencyCode;
import com.helma.helmabackend.entity.crowdfunding.enums.ProjectStage;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "application_raise")
@Getter
@Setter
public class ApplicationRaise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrowdfundingType type;

    @Column(name = "business_name", length = 160)
    private String businessName;


    @Column(length = 255)
    private String website;

    @Column(length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CurrencyCode currency = CurrencyCode.TND;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private SubSector subSector;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "application_raise_tags",
            joinColumns = @JoinColumn(name = "application_raise_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 80)
    private Set<AppTag> tags = new LinkedHashSet<>();

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "problem_statement", columnDefinition = "TEXT")
    private String problemStatement;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(name = "target_customers", columnDefinition = "TEXT")
    private String targetCustomers;

    @Column(name = "use_of_funds", columnDefinition = "TEXT")
    private String useOfFunds;

    @Column(name = "funding_goal", precision = 15, scale = 3, nullable = false)
    private BigDecimal fundingGoal = BigDecimal.ZERO;

    @Column(name = "investors_pledged_amount", precision = 15, scale = 3, nullable = false)
    private BigDecimal investorsPledgedAmount = BigDecimal.ZERO;

    @Column(name = "customer_count")
    private Integer customerCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ProjectStage stage;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(length = 80)
    private String governorate;

    @Column(length = 80)
    private String city;

    @Column(name = "contact_first_name", length = 80)
    private String contactFirstName;

    @Column(name = "contact_last_name", length = 80)
    private String contactLastName;

    @Column(name = "contact_title", length = 120)
    private String contactTitle;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "use_profile_contact", nullable = false)
    private boolean useProfileContact = true;

    @Column(name = "accepted_terms", nullable = false)
    private boolean acceptedTerms = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationRaiseStatus status = ApplicationRaiseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "draft_step", nullable = false, length = 20)
    private ApplicationRaiseDraftStep draftStep = ApplicationRaiseDraftStep.CONTACT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.fundingGoal == null) {
            this.fundingGoal = BigDecimal.ZERO;
        }
        if (this.investorsPledgedAmount == null) {
            this.investorsPledgedAmount = BigDecimal.ZERO;
        }
        if (this.currency == null) {
            this.currency = CurrencyCode.TND;
        }
        if (this.status == null) {
            this.status = ApplicationRaiseStatus.DRAFT;
        }
        if (this.draftStep == null) {
            this.draftStep = ApplicationRaiseDraftStep.CONTACT;
        }
        if (this.tags == null) {
            this.tags = new LinkedHashSet<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();

        if (this.fundingGoal == null) {
            this.fundingGoal = BigDecimal.ZERO;
        }
        if (this.investorsPledgedAmount == null) {
            this.investorsPledgedAmount = BigDecimal.ZERO;
        }
        if (this.currency == null) {
            this.currency = CurrencyCode.TND;
        }
        if (this.status == null) {
            this.status = ApplicationRaiseStatus.DRAFT;
        }
        if (this.draftStep == null) {
            this.draftStep = ApplicationRaiseDraftStep.CONTACT;
        }
        if (this.tags == null) {
            this.tags = new LinkedHashSet<>();
        }
    }
}
