package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.CurrencyCode;
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
    @Column(nullable = false, length = 20)
    private CrowdfundingType type;

    @Column(nullable = false, length = 160)
    private String businessName;

    @Column(length = 120, unique = true)
    private String companyNumber;

    @Column(length = 255)
    private String website;

    @Column(length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CurrencyCode currency = CurrencyCode.TND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
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

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal fundingGoal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal investorsPledgedAmount = BigDecimal.ZERO;

    private Integer customerCount;

    @Column(length = 80)
    private String contactFirstName;

    @Column(length = 80)
    private String contactLastName;

    @Column(length = 120)
    private String contactTitle;

    @Column(length = 180)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(nullable = false)
    private boolean acceptedTerms = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationRaiseStatus status = ApplicationRaiseStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.fundingGoal == null) this.fundingGoal = BigDecimal.ZERO;
        if (this.investorsPledgedAmount == null) this.investorsPledgedAmount = BigDecimal.ZERO;
        if (this.currency == null) this.currency = CurrencyCode.TND;
        if (this.status == null) this.status = ApplicationRaiseStatus.DRAFT;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}