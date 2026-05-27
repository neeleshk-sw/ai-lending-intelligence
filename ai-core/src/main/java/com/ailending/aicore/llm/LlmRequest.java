package com.ailending.aicore.llm;

/**
 * Request DTO for LLM generation.
 * Carries prompt, model selection, and generation parameters.
 * Use {@link Builder} to construct instances.
 */
public class LlmRequest {

    private final String prompt;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    private LlmRequest(String prompt, String model, double temperature, int maxTokens) {
        this.prompt = prompt;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public String getPrompt() { return prompt; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }

    @Override
    public String toString() {
        return "LlmRequest{model='" + model + "', temperature=" + temperature
                + ", maxTokens=" + maxTokens + ", promptLength=" + prompt.length() + "}";
    }

    /**
     * Fluent builder for LlmRequest.
     */
    public static class Builder {
        private String prompt;
        private String model = "llama3:8b";
        private double temperature = 0.7;
        private int maxTokens = 512;

        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder temperature(double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }

        public LlmRequest build() {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("Prompt must not be null or blank");
            }
            return new LlmRequest(prompt, model, temperature, maxTokens);
        }
    }
}
