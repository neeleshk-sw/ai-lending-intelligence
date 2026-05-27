package com.ailending.aicore.embedding;

/**
 * Request DTO for embedding generation.
 * Carries text content and model parameters.
 */
public class EmbeddingRequest {

    private final String text;
    private final String model;

    public EmbeddingRequest(String text, String model) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text content must not be null or blank");
        }
        this.text = text;
        this.model = model != null ? model : "bge-small-en-v1.5";
    }

    public String getText() {
        return text;
    }

    public String getModel() {
        return model;
    }
}
