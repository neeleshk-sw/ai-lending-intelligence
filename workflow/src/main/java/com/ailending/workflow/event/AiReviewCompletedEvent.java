package com.ailending.workflow.event;

import java.time.Instant;

public class AiReviewCompletedEvent extends WorkflowEvent {

    private final double confidenceScore;
    private final String modelUsed;

    public AiReviewCompletedEvent(String loanId, String customerId, Instant occurredAt,
                                  double confidenceScore, String modelUsed) {
        super(loanId, customerId, occurredAt);
        this.confidenceScore = confidenceScore;
        this.modelUsed       = modelUsed;
    }

    public double getConfidenceScore() { return confidenceScore; }
    public String getModelUsed()       { return modelUsed; }
}
