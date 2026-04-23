package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.CurrencyCode;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ApplicationRaiseResponse {
    public Long id;
    public Long ownerUserId;
    public CrowdfundingType type;

    public String businessName;
    public String companyNumber;
    public String website;

    public String country;
    public CurrencyCode currency;

    public Sector sector;
    public SubSector subSector;
    public Set<AppTag> tags = new LinkedHashSet<>();
    public String summary;

    public BigDecimal fundingGoal;
    public BigDecimal investorsPledgedAmount;
    public Integer customerCount;

    public String contactFirstName;
    public String contactLastName;
    public String contactTitle;
    public String contactEmail;
    public String contactPhone;

    public boolean acceptedTerms;
    public ApplicationRaiseStatus status;

    public EquityDetailResponse equityDetail;
    public List<ApplicationDocumentResponse> documents;

    public Instant createdAt;
    public Instant updatedAt;
}