package com.ailending.app.controller.dto.workflow;

public class CompleteAiReviewRequest {
    private double confidenceScore;
    private String modelUsed;

    public double getConfidenceScore()          { return confidenceScore; }
    public void setConfidenceScore(double v)    { this.confidenceScore = v; }
    public String getModelUsed()                { return modelUsed; }
    public void setModelUsed(String v)          { this.modelUsed = v; }
}
