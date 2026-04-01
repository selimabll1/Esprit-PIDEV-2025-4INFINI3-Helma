package com.helma.helmabackend.dto.crowdfunding.ai;

import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class AiCampaignInsightRequest {
    public Long campaignId;
    public String businessName;
    public CrowdfundingType type;
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

    @Override
    public String toString() {
        return "AiCampaignInsightRequest{" +
                "campaignId=" + campaignId +
                ", businessName='" + businessName + '\'' +
                ", type=" + type +
                ", sector=" + sector +
                ", subSector=" + subSector +
                ", tags=" + tags +
                ", summary='" + summary + '\'' +
                ", fundingGoal=" + fundingGoal +
                ", investorsPledgedAmount=" + investorsPledgedAmount +
                ", currency='" + currency + '\'' +
                ", equityOfferedPercent=" + equityOfferedPercent +
                ", preMoneyValuation=" + preMoneyValuation +
                ", minInvestment=" + minInvestment +
                '}';
    }
}