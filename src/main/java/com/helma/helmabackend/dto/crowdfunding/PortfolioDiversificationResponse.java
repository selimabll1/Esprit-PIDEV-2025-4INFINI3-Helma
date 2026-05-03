package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioDiversificationResponse {
    public Integer distinctSectors = 0;
    public Integer distinctSubSectors = 0;
    public Integer distinctRegions = 0;
    public Integer distinctTags = 0;

    public BigDecimal largestPositionWeightPct = BigDecimal.ZERO;
    public BigDecimal top3PositionsWeightPct = BigDecimal.ZERO;
    public BigDecimal largestSectorWeightPct = BigDecimal.ZERO;
    public BigDecimal largestRegionWeightPct = BigDecimal.ZERO;

    /**
     * HHI expressed on a 0-1 scale.
     * Example: 50%, 30%, 20% => (50² + 30² + 20²) / 10000 = 0.38.
     */
    public BigDecimal concentrationIndexHhi = BigDecimal.ZERO;
    public BigDecimal sectorConcentrationHhi = BigDecimal.ZERO;
    public BigDecimal regionConcentrationHhi = BigDecimal.ZERO;

    public BigDecimal effectiveNumberOfPositions = BigDecimal.ZERO;
    public BigDecimal effectiveNumberOfSectors = BigDecimal.ZERO;
    public BigDecimal effectiveNumberOfRegions = BigDecimal.ZERO;

    public Integer breadthPenalty = 0;
    public Integer positionConcentrationPenalty = 0;
    public Integer sectorConcentrationPenalty = 0;
    public Integer regionConcentrationPenalty = 0;

    public Integer diversificationScore = 0;
}
