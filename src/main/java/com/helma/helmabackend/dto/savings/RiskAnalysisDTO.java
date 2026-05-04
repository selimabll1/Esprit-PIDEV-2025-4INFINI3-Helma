package com.helma.helmabackend.dto.savings;

public class RiskAnalysisDTO {
    private Long goalId;
    private String goalTitle;
    private String userName;
    private String riskLevel; // HIGH, MEDIUM, LOW, INACTIVE
    private String reason;
    private String recommendation;

    public RiskAnalysisDTO() {}

    public RiskAnalysisDTO(Long goalId, String goalTitle, String userName, String riskLevel, String reason, String recommendation) {
        this.goalId = goalId;
        this.goalTitle = goalTitle;
        this.userName = userName;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.recommendation = recommendation;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getGoalTitle() {
        return goalTitle;
    }

    public void setGoalTitle(String goalTitle) {
        this.goalTitle = goalTitle;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
