package com.helma.helmabackend.dto.crowdfunding;

public class PortfolioInsightResponse {
    public String type;
    public String title;
    public String message;

    public PortfolioInsightResponse() {
    }

    public PortfolioInsightResponse(String type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
    }
}
