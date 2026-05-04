package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseDraftStep;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.CurrencyCode;
import com.helma.helmabackend.entity.crowdfunding.enums.ProjectStage;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminApplicationRaiseSummaryResponse {
    public Long id;
    public Long ownerUserId;
    public CrowdfundingType type;

    public String businessName;
    public String website;

    public String country;
    public CurrencyCode currency;

    public Sector sector;
    public SubSector subSector;
    public Set<AppTag> tags = new LinkedHashSet<>();

    public ProjectStage stage;

    public String summary;
    public String problemStatement;
    public String solution;
    public String targetCustomers;
    public String useOfFunds;

    public BigDecimal fundingGoal;
    public BigDecimal investorsPledgedAmount;
    public BigDecimal raisedAmount;
    public BigDecimal remainingAmount;
    public BigDecimal fundingProgressPercent;

    public Integer customerCount;
    public Integer teamSize;
    public String governorate;
    public String city;

    public String contactFirstName;
    public String contactLastName;
    public String contactTitle;
    public String contactEmail;
    public String contactPhone;

    public boolean useProfileContact;
    public boolean acceptedTerms;

    public ApplicationRaiseStatus status;
    public ApplicationRaiseDraftStep draftStep;

    public Integer applicationCompletionPercent;
    public Integer documentCompletionPercent;

    public EquityDetailResponse equityDetail;
    public List<ApplicationDocumentResponse> documents;

    public Instant createdAt;
    public Instant updatedAt;
    public Instant submittedAt;
}