package com.helma.helmabackend.dto.crowdfunding.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AiCampaignInsightResponse {
    public Long campaignId;

    public String matchedSector;
    public String matchedSubSector;
    public String selectedModel;
    public String behaviorClass;
    public String forecastBucket;
    public String confidenceBand;
    public BigDecimal rankingScore;
    public BigDecimal currentInterestLevel;
    public BigDecimal nearTermGrowth;
    public BigDecimal quarterGrowth;
    public BigDecimal halfYearGrowth;
    public BigDecimal fullYearGrowth;
    public BigDecimal resilienceScore;
    public BigDecimal momentumScore;
    public BigDecimal attractivenessScore;
    public BigDecimal riskScore;
    public BigDecimal trendAlignmentScore;
    public BigDecimal valuationPressureScore;
    public String overallLabel;
    public String explanation;
    public List<AiForecastPointResponse> forecastPoints = new ArrayList<>();

}