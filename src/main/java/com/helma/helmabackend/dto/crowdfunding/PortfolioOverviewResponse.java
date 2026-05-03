package com.helma.helmabackend.dto.crowdfunding;

import java.util.ArrayList;
import java.util.List;

public class PortfolioOverviewResponse {
    public PortfolioSummaryResponse summary = new PortfolioSummaryResponse();
    public PortfolioDiversificationResponse diversification = new PortfolioDiversificationResponse();
    public PortfolioOptimizationMetricsResponse optimization = new PortfolioOptimizationMetricsResponse();

    public List<PortfolioAllocationItemResponse> sectorAllocation = new ArrayList<>();
    public List<PortfolioAllocationItemResponse> subSectorAllocation = new ArrayList<>();
    public List<PortfolioAllocationItemResponse> regionAllocation = new ArrayList<>();
    public List<PortfolioAllocationItemResponse> tagExposure = new ArrayList<>();

    public List<PortfolioInsightResponse> insights = new ArrayList<>();
    public List<PortfolioPositionResponse> positions = new ArrayList<>();
}
