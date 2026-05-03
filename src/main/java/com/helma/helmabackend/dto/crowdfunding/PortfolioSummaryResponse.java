package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioSummaryResponse {
    public Long investorUserId;
    public String currency;

    /** Confirmed paid equity investments only. */
    public BigDecimal totalInvested = BigDecimal.ZERO;
    public Integer activePositions = 0;
    public BigDecimal averageTicket = BigDecimal.ZERO;

    /** Confirmed + pending equity commitments. */
    public BigDecimal committedCapital = BigDecimal.ZERO;

    /** Confirmed invested / committed capital. */
    public BigDecimal deploymentRatePct = BigDecimal.ZERO;

    /** Pledges that still need payment completion; not counted in totalInvested. */
    public BigDecimal pendingCommitments = BigDecimal.ZERO;
    public Integer pendingCommitmentsCount = 0;
    public BigDecimal pendingCommitmentWeightPct = BigDecimal.ZERO;

    /** Failed, cancelled, or refunded pledge amount; not counted in totalInvested. */
    public BigDecimal failedOrCanceledAmount = BigDecimal.ZERO;
    public Integer failedOrCanceledCount = 0;

    public BigDecimal largestPositionWeightPct = BigDecimal.ZERO;
    public BigDecimal top3PositionsWeightPct = BigDecimal.ZERO;
    public BigDecimal largestSectorWeightPct = BigDecimal.ZERO;
    public BigDecimal largestRegionWeightPct = BigDecimal.ZERO;

    /** Kept explicit because portfolio analytics is now investment/equity-oriented. */
    public BigDecimal equityInvested = BigDecimal.ZERO;
    public BigDecimal equityAllocationPct = BigDecimal.ZERO;

    public Integer distinctSectors = 0;
    public Integer distinctSubSectors = 0;
    public Integer distinctRegions = 0;
    public Integer distinctTags = 0;

    public BigDecimal concentrationIndexHhi = BigDecimal.ZERO;
    public BigDecimal sectorConcentrationHhi = BigDecimal.ZERO;
    public BigDecimal regionConcentrationHhi = BigDecimal.ZERO;
    public BigDecimal effectiveNumberOfPositions = BigDecimal.ZERO;
    public BigDecimal effectiveNumberOfSectors = BigDecimal.ZERO;
    public BigDecimal effectiveNumberOfRegions = BigDecimal.ZERO;
    public Integer diversificationScore = 0;

    public BigDecimal averageOwnershipPercent = BigDecimal.ZERO;
    public BigDecimal maxOwnershipPercent = BigDecimal.ZERO;
    public BigDecimal weightedAverageFundingProgressPct = BigDecimal.ZERO;

    public String portfolioHealth = "NO_POSITIONS";
    public String concentrationRisk = "NO_POSITIONS";
}
