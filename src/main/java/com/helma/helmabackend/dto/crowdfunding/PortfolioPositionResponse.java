package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class PortfolioPositionResponse {
    public Long campaignId;
    public String campaignBusinessName;
    public CrowdfundingType campaignType;

    public Sector sector;
    public SubSector subSector;
    public Set<AppTag> tags = new LinkedHashSet<>();

    public BigDecimal investedAmount = BigDecimal.ZERO;
    public String currency;

    public BigDecimal positionWeightPct = BigDecimal.ZERO;

    public BigDecimal campaignFundingGoal;
    public BigDecimal campaignRaisedAmount;
    public BigDecimal campaignFundingProgressPct = BigDecimal.ZERO;

    public BigDecimal minInvestment;
    public BigDecimal equityOfferedPercent;
    public BigDecimal preMoneyValuation;
    public BigDecimal postMoneyValuation;
    public BigDecimal ownershipPercent;

    public Instant pledgedAt;
}