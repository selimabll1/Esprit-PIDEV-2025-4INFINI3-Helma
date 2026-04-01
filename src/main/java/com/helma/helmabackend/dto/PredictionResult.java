package com.helma.helmabackend.dto;

public class PredictionResult {
    private String prediction;
    private double default_probability;
    private String risk_level;
    private double confidence;

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }
    public double getDefault_probability() { return default_probability; }
    public void setDefault_probability(double default_probability) { this.default_probability = default_probability; }
    public String getRisk_level() { return risk_level; }
    public void setRisk_level(String risk_level) { this.risk_level = risk_level; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
