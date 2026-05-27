package com.ailending.aicore.llm;

/**
 * Response DTO from LLM generation.
 * Contains generated text, model info, and performance metrics.
 */
public class LlmResponse {

    private final String text;
    private final String model;
    private final int tokenCount;
    private final long durationMs;

    public LlmResponse(String text, String model, int tokenCount, long durationMs) {
        this.text = text;
        this.model = model;
        this.tokenCount = tokenCount;
        this.durationMs = durationMs;
    }

    public String getText() { return text; }
    public String getModel() { return model; }
    public int getTokenCount() { return tokenCount; }
    public long getDurationMs() { return durationMs; }

    @Override
    public String toString() {
        return "LlmResponse{model='" + model + "', tokenCount=" + tokenCount
                + ", durationMs=" + durationMs + "}";
    }
}
