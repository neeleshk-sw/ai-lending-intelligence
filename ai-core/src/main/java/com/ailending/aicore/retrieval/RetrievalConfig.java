package com.ailending.aicore.retrieval;

/**
 * Configuration DTO for fine-tuning retrieval logic.
 */
public class RetrievalConfig {

    private final double similarityThreshold;
    private final int defaultTopK;

    public RetrievalConfig(double similarityThreshold, int defaultTopK) {
        this.similarityThreshold = similarityThreshold;
        this.defaultTopK = defaultTopK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }
}
