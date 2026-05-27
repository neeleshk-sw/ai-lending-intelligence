package com.ailending.aicore.embedding;

import java.util.List;

/**
 * Abstraction for text-to-vector embedding.
 * Implementations must handle communication with specific embedding backends.
 */
public interface EmbeddingProvider {

    /**
     * Generates a vector embedding for the text provided in the request.
     *
     * @param request the request containing target text and configuration parameters
     * @return the generated embedding vector and dimension
     * @throws EmbeddingException if generation fails
     */
    EmbeddingResult embed(EmbeddingRequest request);

    /**
     * Generates embeddings for a batch of requests in a single call.
     * Implementations should use the backend's native batch API where available
     * to avoid per-request round-trip overhead during ingestion.
     *
     * @param requests list of embedding requests to process
     * @return list of results in the same order as the input requests
     * @throws EmbeddingException if any embedding in the batch fails
     */
    List<EmbeddingResult> embed(List<EmbeddingRequest> requests);

    /**
     * Returns the fixed output dimensionality of vectors produced by this provider.
     * Used at startup to verify compatibility with the configured vector store index.
     *
     * @return the number of dimensions in each output vector (e.g. 384, 768, 1536)
     */
    int getDimensions();

    /**
     * Checks if the embedding backend is currently available.
     * Supports offline-first behavior.
     *
     * @return true if available, false otherwise
     */
    boolean isAvailable();
}
