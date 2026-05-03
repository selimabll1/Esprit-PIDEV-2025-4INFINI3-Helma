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

    /** HELMA = real paid Helma pledge. IMPORTED_XLSX = externally tracked row uploaded by investor. */
    public String positionSource = "HELMA";
    public Boolean imported = false;
    public String sourceReference;
    public String notes;
    public Instant importedAt;

    public Sector sector;
    public SubSector subSector;
    public String governorate;
    public String city;
    public Set<AppTag> tags = new LinkedHashSet<>();

    public BigDecimal investedAmount = BigDecimal.ZERO;
    public String currency;

    public BigDecimal positionWeightPct = BigDecimal.ZERO;
    public String concentrationRisk = "LOW";

    public BigDecimal campaignFundingGoal;
    public BigDecimal campaignRaisedAmount;
    public BigDecimal campaignFundingProgressPct = BigDecimal.ZERO;
    public BigDecimal campaignFundingGap = BigDecimal.ZERO;
    public BigDecimal campaignFundingGapPct = BigDecimal.ZERO;

    public BigDecimal minInvestment;
    public BigDecimal equityOfferedPercent;
    public BigDecimal preMoneyValuation;
    public BigDecimal postMoneyValuation;
    public BigDecimal ownershipPercent;

    public Instant pledgedAt;
}
