package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PortfolioDiversificationResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioOverviewResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioPositionResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioSummaryResponse;
import com.helma.helmabackend.service.crowdfunding.PortfolioAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioAnalyticsController {

    private final PortfolioAnalyticsService portfolioAnalyticsService;

    @GetMapping("/me/overview")
    public PortfolioOverviewResponse myOverview() {
        return portfolioAnalyticsService.getMyOverview();
    }

    @GetMapping("/me/summary")
    public PortfolioSummaryResponse mySummary() {
        return portfolioAnalyticsService.getMySummary();
    }

    @GetMapping("/me/positions")
    public List<PortfolioPositionResponse> myPositions() {
        return portfolioAnalyticsService.getMyPositions();
    }

    @GetMapping("/me/diversification")
    public PortfolioDiversificationResponse myDiversification() {
        return portfolioAnalyticsService.getMyDiversification();
    }
}