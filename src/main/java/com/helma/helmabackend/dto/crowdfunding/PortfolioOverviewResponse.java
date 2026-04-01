package com.helma.helmabackend.dto.crowdfunding;

import java.util.ArrayList;
import java.util.List;

public class PortfolioOverviewResponse {
    public PortfolioSummaryResponse summary = new PortfolioSummaryResponse();
    public PortfolioDiversificationResponse diversification = new PortfolioDiversificationResponse();
    public List<PortfolioAllocationItemResponse> sectorAllocation = new ArrayList<>();
    public List<PortfolioAllocationItemResponse> subSectorAllocation = new ArrayList<>();
    public List<PortfolioAllocationItemResponse> tagExposure = new ArrayList<>();
    public List<PortfolioPositionResponse> positions = new ArrayList<>();
}