package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightResponse;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class CampaignResponse {
    public Long id;
    public CrowdfundingType type;

    public String businessName;
    public String website;

    public Sector sector;
    public SubSector subSector;
    public Set<AppTag> tags = new LinkedHashSet<>();

    public String summary;

    public BigDecimal fundingGoal;
    public BigDecimal investorsPledgedAmount;
    public String currency;

    public BigDecimal equityOfferedPercent;
    public BigDecimal preMoneyValuation;
    public BigDecimal minInvestment;

    public Instant createdAt;
    public Instant updatedAt;

    public AiCampaignInsightResponse aiInsights;
    public boolean aiUnavailable;
    public BigDecimal trendScore;
    public String trendLabel;
}