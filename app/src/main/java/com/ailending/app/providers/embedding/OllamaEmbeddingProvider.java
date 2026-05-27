package com.ailending.app.providers.embedding;

import com.ailending.aicore.embedding.EmbeddingException;
import com.ailending.aicore.embedding.EmbeddingProvider;
import com.ailending.aicore.embedding.EmbeddingRequest;
import com.ailending.aicore.embedding.EmbeddingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of EmbeddingProvider using Spring AI's EmbeddingModel (configured for Ollama).
 * Lives in the app module so that ai-core remains Spring-free.
 */
@Service
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingProvider.class);

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            List<Double> vector = embeddingModel.embed(request.getText());
            long durationMs = System.currentTimeMillis() - startTime;

            int dimension = vector != null ? vector.size() : 0;
            return new EmbeddingResult(vector, dimension, durationMs);
        } catch (Exception e) {
            log.error("Failed to generate embedding using Ollama", e);
            throw new EmbeddingException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Batch embedding: processes each request individually and collects results.
     * Spring AI's Ollama EmbeddingModel does not expose a native batch API in the
     * M1 milestone, so requests are issued sequentially. A future implementation
     * can replace this with a parallel stream or a dedicated batch endpoint when
     * the upstream library supports it.
     */
    @Override
    public List<EmbeddingResult> embed(List<EmbeddingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        try {
            long startTime = System.currentTimeMillis();
            List<EmbeddingResult> results = new java.util.ArrayList<>(requests.size());
            for (EmbeddingRequest request : requests) {
                List<Double> vector = embeddingModel.embed(request.getText());
                int dimension = vector != null ? vector.size() : 0;
                // Per-item duration is not tracked individually to avoid clock overhead;
                // durationMs here reflects wall-clock time from the start of the batch.
                long elapsed = System.currentTimeMillis() - startTime;
                results.add(new EmbeddingResult(vector, dimension, elapsed));
            }
            log.debug("Batch embedded {} documents in {}ms", requests.size(),
                    System.currentTimeMillis() - startTime);
            return results;
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings using Ollama", e);
            throw new EmbeddingException("Batch embedding generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the output dimensionality reported by the underlying EmbeddingModel.
     * Spring AI's EmbeddingModel.dimensions() queries the model metadata from Ollama.
     */
    @Override
    public int getDimensions() {
        return embeddingModel.dimensions();
    }

    @Override
    public boolean isAvailable() {
        return embeddingModel != null;
    }
}
