package com.ailending.aicore.rag;

import com.ailending.aicore.vectorstore.SearchResult;
import java.util.List;

/**
 * Response DTO returning outputs from RAG execution.
 */
public class RagResponse {

    private final String answer;
    private final List<SearchResult> sourceReferences;
    private final String modelUsed;
    private final double confidenceScore;
    private final long durationMs;

    public RagResponse(String answer, List<SearchResult> sourceReferences, String modelUsed, double confidenceScore, long durationMs) {
        this.answer = answer;
        this.sourceReferences = sourceReferences;
        this.modelUsed = modelUsed;
        this.confidenceScore = confidenceScore;
        this.durationMs = durationMs;
    }

    public String getAnswer() {
        return answer;
    }

    public List<SearchResult> getSourceReferences() {
        return sourceReferences;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
