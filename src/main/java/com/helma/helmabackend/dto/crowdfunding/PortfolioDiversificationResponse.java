package com.helma.helmabackend.dto.crowdfunding;

import java.math.BigDecimal;

public class PortfolioDiversificationResponse {
    public Integer distinctSectors = 0;
    public Integer distinctSubSectors = 0;
    public Integer distinctTags = 0;

    public BigDecimal largestPositionWeightPct = BigDecimal.ZERO;
    public BigDecimal top3PositionsWeightPct = BigDecimal.ZERO;
    public BigDecimal largestSectorWeightPct = BigDecimal.ZERO;

    /**
     * Standard HHI style using percentage weights squared.
     * Example:
     * 50%, 30%, 20% => 50^2 + 30^2 + 20^2 = 3800
     */
    public BigDecimal concentrationIndexHhi = BigDecimal.ZERO;

    public Integer breadthPenalty = 0;
    public Integer positionConcentrationPenalty = 0;
    public Integer sectorConcentrationPenalty = 0;

    public Integer diversificationScore = 0;
}