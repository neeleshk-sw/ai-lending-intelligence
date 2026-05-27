package com.ailending.aicore.embedding;

import java.util.List;

/**
 * Result DTO representing generated embedding vectors.
 */
public class EmbeddingResult {

    private final List<Double> vector;
    private final int dimension;
    private final long durationMs;

    public EmbeddingResult(List<Double> vector, int dimension, long durationMs) {
        this.vector = vector;
        this.dimension = dimension;
        this.durationMs = durationMs;
    }

    public List<Double> getVector() {
        return vector;
    }

    public int getDimension() {
        return dimension;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
