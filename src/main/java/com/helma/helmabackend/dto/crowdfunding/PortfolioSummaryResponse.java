package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioSummaryResponse {
    public Long investorUserId;
    public String currency;

    public BigDecimal totalInvested = BigDecimal.ZERO;
    public Integer activePositions = 0;
    public BigDecimal averageTicket = BigDecimal.ZERO;

    public BigDecimal largestPositionWeightPct = BigDecimal.ZERO;
    public BigDecimal top3PositionsWeightPct = BigDecimal.ZERO;
    public BigDecimal largestSectorWeightPct = BigDecimal.ZERO;

    public BigDecimal equityInvested = BigDecimal.ZERO;
    public BigDecimal donationInvested = BigDecimal.ZERO;

    public BigDecimal equityAllocationPct = BigDecimal.ZERO;
    public BigDecimal donationAllocationPct = BigDecimal.ZERO;

    public Integer distinctSectors = 0;
    public Integer distinctSubSectors = 0;
    public Integer distinctTags = 0;

    public BigDecimal concentrationIndexHhi = BigDecimal.ZERO;
    public Integer diversificationScore = 0;
}