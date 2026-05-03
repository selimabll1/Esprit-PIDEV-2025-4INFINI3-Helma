package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioOptimizationMetricsResponse {
    /** Confirmed + pending equity commitments. */
    public BigDecimal committedCapital = BigDecimal.ZERO;

    /** Confirmed invested / committed capital. */
    public BigDecimal deploymentRatePct = BigDecimal.ZERO;

    /** Pending commitments / committed capital. */
    public BigDecimal pendingCommitmentWeightPct = BigDecimal.ZERO;

    /** Weighted by each confirmed position amount. */
    public BigDecimal weightedAverageFundingProgressPct = BigDecimal.ZERO;

    /** Average of position weights; useful for sanity-checking concentration. */
    public BigDecimal averagePositionWeightPct = BigDecimal.ZERO;

    /** 1 / HHI. Higher means capital behaves like it is spread across more equal positions. */
    public BigDecimal effectiveNumberOfPositions = BigDecimal.ZERO;

    /** 1 / sector HHI. Higher means sector exposure is more balanced. */
    public BigDecimal effectiveNumberOfSectors = BigDecimal.ZERO;

    /** 1 / region HHI. Higher means region exposure is more balanced. */
    public BigDecimal effectiveNumberOfRegions = BigDecimal.ZERO;

    public BigDecimal sectorConcentrationHhi = BigDecimal.ZERO;
    public BigDecimal regionConcentrationHhi = BigDecimal.ZERO;

    public BigDecimal largestRegionWeightPct = BigDecimal.ZERO;

    /** Simple label derived from diversification score and concentration thresholds. */
    public String portfolioHealth = "NO_POSITIONS";

    public String concentrationRisk = "NO_POSITIONS";

    /** Suggested max single-position weight for early-stage private portfolios. */
    public BigDecimal suggestedMaxPositionWeightPct = new BigDecimal("35.000");

    /** How many confirmed positions are above suggestedMaxPositionWeightPct. */
    public Integer overweightPositions = 0;
}
