package com.ailending.aicore.llm;

/**
 * Abstraction for LLM interactions.
 * Implementations must handle communication with specific LLM backends (e.g., Ollama).
 */
public interface LlmProvider {

    /**
     * Generates a response from the LLM based on the provided request.
     *
     * @param request the generation request containing prompt and parameters
     * @return the generation response containing text and metrics
     * @throws LlmException if the provider is unavailable or generation fails
     */
    LlmResponse generate(LlmRequest request);

    /**
     * Checks if the LLM backend is currently reachable and ready to serve requests.
     * Supports offline-first graceful degradation.
     *
     * @return true if available, false otherwise
     */
    boolean isAvailable();
}
