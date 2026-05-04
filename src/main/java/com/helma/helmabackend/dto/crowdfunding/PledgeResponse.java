package com.helma.helmabackend.dto.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.PledgeStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PledgeResponse {
    public Long id;
    public Long applicationRaiseId;
    public Long backerUserId;

    public BigDecimal amount;
    public String currency;
    public String message;
    public PledgeStatus status;

    public String campaignBusinessName;
    public CrowdfundingType campaignType;
    public BigDecimal campaignFundingGoal;
    public BigDecimal campaignInvestorsPledgedAmount;
    public BigDecimal campaignMinInvestment;

    public Instant createdAt;
    public Instant updatedAt;
}
